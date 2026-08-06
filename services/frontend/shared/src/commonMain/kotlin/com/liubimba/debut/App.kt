package com.liubimba.debut

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

import debut.shared.generated.resources.Res
import debut.shared.generated.resources.compose_multiplatform
import debut.shared.generated.resources.nav_library
import debut.shared.generated.resources.nav_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class Screen(val title: StringResource){
    Library(Res.string.nav_library),
    Settings(Res.string.nav_settings)
}

@Composable
@Preview
fun App() {
    val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors){
        var current by remember { mutableStateOf(Screen.Library) }
        BoxWithConstraints  (Modifier.fillMaxSize()){
            if(maxWidth >= 720.dp){
                Row(Modifier.fillMaxSize()){
                    NavigationRail {
                        for (screen in Screen.entries) {
                            NavigationRailItem(
                                selected = current == screen,
                                onClick = {current = screen},
                                icon = {Text(stringResource( screen.title).take(1))},
                                label = {Text(stringResource( screen.title))}
                            )
                        }
                    }

                }
            }else{
                Scaffold (
                    bottomBar = {
                        NavigationBar {
                            for (screen in Screen.entries) {
                                NavigationBarItem(
                                    selected = current == screen,
                                    onClick = {current=screen},
                                    icon = {Text(stringResource( screen.title).take(1))},
                                    label={Text(stringResource( screen.title))}
                                )
                            }
                        }
                    }
                ){paddingValues ->
                    ScreenHost(current, Modifier.fillMaxSize().padding(paddingValues))
                }
            }
        }
    }
}

@Composable
private fun ScreenHost(screen: Screen, modifier: Modifier = Modifier){
    Box(modifier, contentAlignment = Alignment.Center){
        Text(stringResource( screen.title), style = MaterialTheme.typography.headlineMedium)
    }
}
