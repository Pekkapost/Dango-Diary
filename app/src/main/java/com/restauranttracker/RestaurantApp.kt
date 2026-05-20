package com.restauranttracker

import android.app.Application
import com.restauranttracker.data.RestaurantDatabase
import com.restauranttracker.util.PhotoStorage

/**
 * Application-scoped singletons. Manual DI in lieu of Hilt — the dependency graph is tiny
 * and a third-party DI framework would add more boilerplate than it removes.
 */
class RestaurantApp : Application() {

    val database: RestaurantDatabase by lazy { RestaurantDatabase.build(this) }
    val photoStorage: PhotoStorage by lazy { PhotoStorage(this) }
}
