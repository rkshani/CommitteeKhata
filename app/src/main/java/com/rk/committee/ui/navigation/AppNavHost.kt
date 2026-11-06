package com.rk.committee.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rk.committee.ui.screens.detail.DetailScreen
import com.rk.committee.ui.screens.edit.EditCommitteeScreen
import com.rk.committee.ui.screens.home.HomeScreen
import com.rk.committee.ui.screens.language.LanguageScreen
import com.rk.committee.ui.screens.settings.SettingsScreen

object Routes {
    const val LANGUAGE = "language"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val DETAIL = "committee/{id}"
    const val EDIT = "edit?id={id}"

    fun detail(id: Long) = "committee/$id"
    fun edit(id: Long = -1L) = "edit?id=$id"
}

@Composable
fun AppNavHost(startOnLanguage: Boolean) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = if (startOnLanguage) Routes.LANGUAGE else Routes.HOME) {
        composable(Routes.LANGUAGE) {
            LanguageScreen(
                onDone = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.LANGUAGE) { inclusive = true } }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onAdd = { nav.navigate(Routes.edit()) },
                onOpen = { nav.navigate(Routes.detail(it)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
        ) {
            EditCommitteeScreen(
                onBack = { nav.popBackStack() },
                onSaved = { id, isNew ->
                    if (isNew) {
                        nav.navigate(Routes.detail(id)) { popUpTo(Routes.HOME) }
                    } else {
                        nav.popBackStack()
                    }
                },
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) {
            DetailScreen(
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.edit(it)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
