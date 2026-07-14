package com.trancong.dexworkspacemanager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trancong.dexworkspacemanager.feature.home.HomeScreen
import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutEditorRoute
import com.trancong.dexworkspacemanager.feature.savedlayouts.SavedLayoutsRoute

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
                    navController.navigate(AppRoute.layoutEditor())
                },
                onSavedLayoutsClick = {
                    navController.navigate(AppRoute.SavedLayouts.route)
                }
            )
        }
        composable(
            route = AppRoute.LayoutEditor.route,
            arguments = listOf(
                navArgument(AppRoute.ARG_WORKSPACE_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val workspaceId = backStackEntry.arguments
                ?.getLong(AppRoute.ARG_WORKSPACE_ID)
                ?.takeIf { it >= 0L }

            LayoutEditorRoute(
                workspaceId = workspaceId,
                onBackClick = navController::popBackStack
            )
        }
        composable(AppRoute.SavedLayouts.route) {
            SavedLayoutsRoute(
                onBackClick = navController::popBackStack,
                onWorkspaceClick = { workspace ->
                    navController.navigate(AppRoute.layoutEditor(workspace.id))
                }
            )
        }
    }
}
