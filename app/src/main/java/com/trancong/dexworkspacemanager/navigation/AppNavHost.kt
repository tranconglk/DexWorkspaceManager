package com.trancong.dexworkspacemanager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trancong.dexworkspacemanager.feature.home.HomeScreen
import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutEditorRoute
import com.trancong.dexworkspacemanager.feature.savedlayouts.SavedLayoutsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route
    ) {
        composable(AppRoute.Home.route) {
            HomeScreen(
                onCreateLayoutClick = {
                    navController.navigate(AppRoute.LayoutEditor.route)
                },
                onSavedLayoutsClick = {
                    navController.navigate(AppRoute.SavedLayouts.route)
                }
            )
        }
        composable(AppRoute.LayoutEditor.route) {
            LayoutEditorRoute(onBackClick = navController::popBackStack)
        }
        composable(AppRoute.SavedLayouts.route) {
            SavedLayoutsScreen(onBackClick = navController::popBackStack)
        }
    }
}
