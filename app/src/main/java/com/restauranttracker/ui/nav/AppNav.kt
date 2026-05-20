package com.restauranttracker.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.restauranttracker.ui.detail.RestaurantDetailScreen
import com.restauranttracker.ui.edit.RestaurantEditScreen
import com.restauranttracker.ui.list.RestaurantListScreen

object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{id}"
    const val EDIT_NEW = "edit/new"
    const val EDIT_EXISTING = "edit/{id}"

    fun detail(id: Long) = "detail/$id"
    fun edit(id: Long) = "edit/$id"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            RestaurantListScreen(
                onAdd = { nav.navigate(Routes.EDIT_NEW) },
                onOpen = { id -> nav.navigate(Routes.detail(id)) },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            RestaurantDetailScreen(
                restaurantId = id,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.edit(id)) },
            )
        }

        composable(Routes.EDIT_NEW) {
            RestaurantEditScreen(
                restaurantId = null,
                onDone = { nav.popBackStack() },
            )
        }

        composable(
            route = Routes.EDIT_EXISTING,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id")
            RestaurantEditScreen(
                restaurantId = id,
                onDone = { nav.popBackStack() },
            )
        }
    }
}
