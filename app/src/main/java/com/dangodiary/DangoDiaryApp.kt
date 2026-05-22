package com.dangodiary

import android.app.Application
import android.util.Log
import com.dangodiary.data.DiaryDatabase
import com.dangodiary.util.AppSettings
import com.dangodiary.util.PhotoStorage
import com.dangodiary.util.buildAppSettings
import com.google.android.libraries.places.api.Places

private const val TAG = "DangoDiaryApp"

/**
 * Application-scoped singletons. Manual DI in lieu of Hilt — the dependency graph is tiny
 * and a third-party DI framework would add more boilerplate than it removes.
 */
class DangoDiaryApp : Application() {

    val database: DiaryDatabase by lazy { DiaryDatabase.build(this) }
    val photoStorage: PhotoStorage by lazy { PhotoStorage(this) }
    val appSettings: AppSettings by lazy { buildAppSettings(this) }

    override fun onCreate() {
        super.onCreate()
        initPlaces()
    }

    private fun initPlaces() {
        val key = BuildConfig.MAPS_API_KEY
        if (key.isBlank()) {
            // No key configured — the address-autocomplete field renders as a no-op tap rather
            // than crashing.
            Log.w(TAG, "MAPS_API_KEY is blank; Places autocomplete will be disabled.")
            return
        }
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, key)
        }
    }
}
