package com.witte.lozify.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.witte.lozify.presentation.archive.ArchiveScreen
import com.witte.lozify.presentation.home.HomeScreen
import com.witte.lozify.presentation.home.HomeViewModel
import com.witte.lozify.presentation.tags.TagEditScreen

/**
 * Navigation graph for Lozify app.
 * Defines all navigation routes and their corresponding screens.
 *
 * Stage 12: Renamed ARCHIVE to TRASH, added TAG_EDIT route.
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
                onNavigateToTrash = {
                    navController.navigate(Routes.TRASH)
                },
                onNavigateToTagEdit = { tagId ->
                    navController.navigate(Routes.tagEdit(tagId))
                }
            )
        }

        composable(Routes.TRASH) {
            ArchiveScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.TAG_EDIT,
            arguments = listOf(
                navArgument("tagId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val tagId = backStackEntry.arguments?.getLong("tagId") ?: return@composable
            val homeViewModel: HomeViewModel = hiltViewModel()
            val allTags by homeViewModel.allTags.collectAsState()

            val tag = allTags.find { it.id == tagId }
            if (tag != null) {
                TagEditScreen(
                    tag = tag,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSave = { newName, newIcon ->
                        homeViewModel.renameTag(tag.id, tag.name, newName, newIcon)
                    }
                )
            }
        }
    }
}
