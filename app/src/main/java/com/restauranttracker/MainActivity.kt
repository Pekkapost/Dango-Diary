package com.restauranttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.restauranttracker.ui.nav.AppNav
import com.restauranttracker.ui.theme.RestaurantTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RestaurantTrackerTheme {
                AppNav()
            }
        }
    }
}
