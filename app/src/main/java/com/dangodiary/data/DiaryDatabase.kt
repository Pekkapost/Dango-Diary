package com.dangodiary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Entry::class],
    version = 2,
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

        // Never use fallbackToDestructiveMigration — losing user data is not an upgrade path.
        fun build(context: Context): DiaryDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                DiaryDatabase::class.java,
                "diary.db",
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
