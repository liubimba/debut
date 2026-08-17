package com.liubimba.debut

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import com.liubimba.debut.ui.AppContainer
import com.liubimba.debut.ui.addsong.AddSongScreen
import com.liubimba.debut.ui.extensions.AppProvider
import com.liubimba.debut.ui.library.LibraryScreen
import com.liubimba.debut.ui.navigation.AddSongRoute
import com.liubimba.debut.ui.navigation.AppScaffold
import com.liubimba.debut.ui.navigation.Destination
import com.liubimba.debut.ui.navigation.LibraryGraph
import com.liubimba.debut.ui.navigation.LibraryRoute
import com.liubimba.debut.ui.navigation.SettingsRoute
import com.liubimba.debut.ui.navigation.SongRoute
import com.liubimba.debut.ui.navigation.toTab
import com.liubimba.debut.ui.settings.SettingsScreen
import com.liubimba.debut.ui.song.SongScreen
import com.liubimba.debut.ui.theme.DebutTheme

private val log = Logger.withTag("App")

@Composable
fun App(container: AppContainer) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentTab = entry?.destination?.toTab() ?: Destination.Library
    DebutTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppProvider {
                AppScaffold(current = currentTab, onSelect = { tab ->
                    log.d { "navigation from $currentTab to $tab" }
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }

                        launchSingleTop = true
                        restoreState = true
                    }
                }) {
                    NavHost(
                        navController = navController,
                        startDestination = LibraryGraph,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        navigation<LibraryGraph>(startDestination = LibraryRoute) {
                            composable<LibraryRoute> {
                                LibraryScreen(
                                    songsRepository = container.songsRepository,
                                    onAddSong = { navController.navigate(AddSongRoute) },
                                    onSelectSong = { song ->
                                        log.d { "opening song $song" }
                                        navController.navigate(SongRoute(song.id))
                                    }
                                )
                            }

                            composable<SongRoute> { backStackEntry ->
                                SongScreen(
                                    id = backStackEntry.toRoute<SongRoute>().id,
                                    songsRepository = container.songsRepository
                                )
                            }
                        }

                        composable<AddSongRoute> {
                            AddSongScreen(
                                songsRepository = container.songsRepository,
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}
