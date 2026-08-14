package com.liubimba.debut

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.liubimba.debut.ui.AppContainer
import com.liubimba.debut.ui.addsong.AddSongScreen
import com.liubimba.debut.ui.extensions.AppProvider
import com.liubimba.debut.ui.library.LibraryScreen
import com.liubimba.debut.ui.navigation.AppScaffold
import com.liubimba.debut.ui.navigation.Destination
import com.liubimba.debut.ui.settings.SettingsScreen
import com.liubimba.debut.ui.theme.DebutTheme

@Composable
@Preview
fun App() {
    DebutTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppProvider {
                var currentScreen by rememberSaveable { mutableStateOf(Destination.Library) }
                AppScaffold(current = currentScreen, onSelect = { currentScreen = it }) {
                    when (currentScreen) {
                        Destination.Library -> LibraryScreen()
                        Destination.Settings -> SettingsScreen()
                        Destination.AddSong -> AddSongScreen(repository = AppContainer.songsRepository)
                    }
                }
            }
        }
    }
}
