package com.witte.lozify.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.witte.lozify.presentation.archive.ArchiveScreen
import com.witte.lozify.presentation.home.HomeScreen

/**
 * Navigation graph for Lozify app.
 * Defines all navigation routes and their corresponding screens.
 */
@Composable
fun LozifyNavGraph(
    navController: NavHostController,
    startDestination: String = Routes.HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToArchive = {
                    navController.navigate(Routes.ARCHIVE)
                }
            )
        }

        composable(Routes.ARCHIVE) {
            ArchiveScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
