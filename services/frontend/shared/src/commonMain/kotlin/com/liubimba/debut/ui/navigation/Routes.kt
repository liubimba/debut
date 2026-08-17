package com.liubimba.debut.ui.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import kotlinx.serialization.Serializable


@Serializable
data object LibraryRoute

@Serializable
data object SettingsRoute

@Serializable
data object AddSongRoute

@Serializable
data object LibraryGraph

@Serializable
data class SongRoute(val id: String)

fun NavDestination.toTab(): Destination? =
    Destination.entries.firstOrNull { tab ->
        hierarchy.any {
            it.hasRoute(tab.route::class)
        }
    }
