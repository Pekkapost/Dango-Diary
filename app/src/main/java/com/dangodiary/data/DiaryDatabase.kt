package com.dangodiary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Entry::class],
    version = 1,
    exportSchema = false,
)
abstract class DiaryDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao

    companion object {
        // When a v2 ships, add a Migration here and pass it to addMigrations(...).
        // Never use fallbackToDestructiveMigration — losing user data is not an upgrade path.
        fun build(context: Context): DiaryDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                DiaryDatabase::class.java,
                "diary.db",
            ).build()
    }
}
