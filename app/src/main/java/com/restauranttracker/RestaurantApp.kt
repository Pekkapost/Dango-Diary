package com.restauranttracker

import android.app.Application
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.restauranttracker.data.RestaurantDatabase
import com.restauranttracker.util.PhotoStorage

/**
 * Application-scoped singletons. Manual DI in lieu of Hilt — the dependency graph is tiny
 * and a third-party DI framework would add more boilerplate than it removes.
 */
class RestaurantApp : Application() {

    val database: RestaurantDatabase by lazy { RestaurantDatabase.build(this) }
    val photoStorage: PhotoStorage by lazy { PhotoStorage(this) }

    override fun onCreate() {
        super.onCreate()
        initPlaces()
    }

    private fun initPlaces() {
        val key = BuildConfig.MAPS_API_KEY
        if (key.isBlank()) {
            // No key configured — Places calls will throw later, but we don't want to crash here.
            // The address-autocomplete field handles the un-initialised case gracefully.
            Log.w("RestaurantApp", "MAPS_API_KEY is blank; Places autocomplete will be disabled.")
            return
        }
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, key)
        }
    }
}
