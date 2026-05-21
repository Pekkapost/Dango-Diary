package com.restauranttracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Restaurant::class],
    version = 3,
    exportSchema = false,
)
abstract class RestaurantDatabase : RoomDatabase() {

    abstract fun restaurantDao(): RestaurantDao

    companion object {
        // v2 redefines `rating` as half-star units (0..10) instead of whole stars (0..5),
        // so existing rows must be doubled to preserve their meaning.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE restaurants SET rating = rating * 2")
            }
        }

        // v3 adds an optional cuisine tag (id from CuisineCatalog).
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE restaurants ADD COLUMN cuisine TEXT")
            }
        }

        // When a v4 ships, add a Migration here and pass it to addMigrations(...).
        // Never use fallbackToDestructiveMigration — losing user data is not an upgrade path.
        fun build(context: Context): RestaurantDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                RestaurantDatabase::class.java,
                "restaurants.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
