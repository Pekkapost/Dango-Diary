package com.restauranttracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Restaurant::class],
    version = 1,
    exportSchema = false,
)
abstract class RestaurantDatabase : RoomDatabase() {

    abstract fun restaurantDao(): RestaurantDao

    companion object {
        // When a v2 ships, add a Migration here and pass it to addMigrations(...).
        // Never use fallbackToDestructiveMigration — losing user data is not an upgrade path.
        fun build(context: Context): RestaurantDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                RestaurantDatabase::class.java,
                "restaurants.db",
            ).build()
    }
}
