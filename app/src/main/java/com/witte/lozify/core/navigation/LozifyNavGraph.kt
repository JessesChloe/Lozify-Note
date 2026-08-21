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
import com.witte.lozify.presentation.backup.BackupRestoreScreen
import com.witte.lozify.presentation.help.HelpCenterScreen
import com.witte.lozify.presentation.home.HomeScreen
import com.witte.lozify.presentation.home.HomeViewModel
import com.witte.lozify.presentation.settings.SettingsScreen
import com.witte.lozify.presentation.tags.TagEditScreen

/**
 * Navigation graph for Lozify app.
 * Defines all navigation routes and their corresponding screens.
 *
 * Stage 12: Renamed ARCHIVE to TRASH, added TAG_EDIT route.
 * Stage 16: Added HELP and BACKUP routes.
 * Stage 17: Added SETTINGS route.
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
                },
                onNavigateToHelp = {
                    navController.navigate(Routes.HELP)
                },
                onNavigateToBackup = {
                    navController.navigate(Routes.BACKUP)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToPro = {
                    navController.navigate(Routes.PRO)
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

        composable(Routes.HELP) {
            HelpCenterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.BACKUP) {
            BackupRestoreScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToBackup = {
                    navController.navigate(Routes.BACKUP)
                },
                onNavigateToHelp = {
                    navController.navigate(Routes.HELP)
                },
                onNavigateToWebDavSync = {
                    navController.navigate(Routes.WEBDAV_SYNC)
                },
                onNavigateToPro = {
                    navController.navigate(Routes.PRO)
                }
            )
        }

        composable(Routes.PRO) {
            com.witte.lozify.presentation.pro.ProMembershipScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.WEBDAV_SYNC) {
            com.witte.lozify.presentation.sync.WebDavSyncScreen(
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
                    onSave = { newName, newIcon, isPinned ->
                        homeViewModel.renameTag(tag.id, tag.name, newName, newIcon)
                        if (tag.isPinned != isPinned) {
                            homeViewModel.togglePinTag(tag.id, isPinned)
                        }
                    }
                )
            }
        }
    }
}
