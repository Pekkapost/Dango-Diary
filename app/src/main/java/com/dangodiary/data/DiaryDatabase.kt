package com.dangodiary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Entry::class],
    version = 4,
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
        // `meal` + `dish_price_cents` columns so existing entries keep their dish data.
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

        // v3 -> v4: drop the legacy `meal` + `dish_price_cents` columns. SQLite below 3.35
        // can't DROP COLUMN, so we use the standard table-recreate pattern (create new, copy
        // rows by named columns, drop old, rename). Dish data lives in `dishes_json` from v3
        // onward; the legacy columns have been written-as-empty by code since then.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE entries_new (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        visited_on INTEGER NOT NULL,
                        rating INTEGER NOT NULL,
                        cuisine TEXT,
                        dishes_json TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        companions TEXT NOT NULL,
                        currency_code TEXT NOT NULL,
                        address_text TEXT,
                        latitude REAL,
                        longitude REAL,
                        photo_paths_json TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO entries_new (
                        id, name, visited_on, rating, cuisine, dishes_json, notes, companions,
                        currency_code, address_text, latitude, longitude, photo_paths_json,
                        created_at
                    )
                    SELECT
                        id, name, visited_on, rating, cuisine, dishes_json, notes, companions,
                        currency_code, address_text, latitude, longitude, photo_paths_json,
                        created_at
                    FROM entries
                """.trimIndent())
                db.execSQL("DROP TABLE entries")
                db.execSQL("ALTER TABLE entries_new RENAME TO entries")
            }
        }

        // Never use fallbackToDestructiveMigration — losing user data is not an upgrade path.
        fun build(context: Context): DiaryDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                DiaryDatabase::class.java,
                "diary.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
