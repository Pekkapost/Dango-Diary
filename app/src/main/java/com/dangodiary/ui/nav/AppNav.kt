package com.dangodiary.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dangodiary.ui.detail.EntryDetailScreen
import com.dangodiary.ui.edit.EntryEditScreen
import com.dangodiary.ui.list.EntryListScreen

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
            EntryListScreen(
                onAdd = { nav.navigate(Routes.EDIT_NEW) },
                onOpen = { id -> nav.navigate(Routes.detail(id)) },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            EntryDetailScreen(
                entryId = id,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.edit(id)) },
            )
        }

        composable(Routes.EDIT_NEW) {
            EntryEditScreen(
                entryId = null,
                onDone = { nav.popBackStack() },
            )
        }

        composable(
            route = Routes.EDIT_EXISTING,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id")
            EntryEditScreen(
                entryId = id,
                onDone = { nav.popBackStack() },
            )
        }
    }
}
