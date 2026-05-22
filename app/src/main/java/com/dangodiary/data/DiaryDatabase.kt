package com.dangodiary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Entry::class],
    version = 3,
    exportSchema = false,
)
abstract class DiaryDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao

    companion object {
        // v1 -> v2: add the `meal` column for the per-entry entree.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN meal TEXT NOT NULL DEFAULT ''")
            }
        }

        // v2 -> v3: introduce per-dish list (`dishes_json`). Backfills from the legacy single
        // `meal` + `dish_price_cents` columns so existing entries keep their dish data. The
        // legacy columns stay on the table for back-compat — a future migration may drop them
        // via a table-recreate once we're confident no rollback path needs them.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN dishes_json TEXT NOT NULL DEFAULT '[]'")
                // Backfill in Kotlin so we don't depend on SQLite JSON functions across API
                // levels. Build the JSON by hand — only two fields, simple escaping.
                db.query("SELECT id, meal, dish_price_cents FROM entries WHERE meal != '' OR dish_price_cents IS NOT NULL").use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        val meal = c.getString(1) ?: ""
                        val priceCents = if (c.isNull(2)) null else c.getLong(2)
                        val nameEsc = meal.replace("\\", "\\\\").replace("\"", "\\\"")
                        val priceJson = priceCents?.toString() ?: "null"
                        val dishJson = "[{\"name\":\"$nameEsc\",\"priceCents\":$priceJson}]"
                        val stmt = db.compileStatement(
                            "UPDATE entries SET dishes_json = ? WHERE id = ?",
                        )
                        stmt.bindString(1, dishJson)
                        stmt.bindLong(2, id)
                        stmt.executeUpdateDelete()
                        stmt.close()
                    }
                }
            }
        }

        // Never use fallbackToDestructiveMigration — losing user data is not an upgrade path.
        fun build(context: Context): DiaryDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                DiaryDatabase::class.java,
                "diary.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
