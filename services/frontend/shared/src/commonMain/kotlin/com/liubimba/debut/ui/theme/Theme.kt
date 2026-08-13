package com.liubimba.debut.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalDebutColors = staticCompositionLocalOf { DebutLightExtraColors }

object DebutTheme {
    val colors: DebutColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDebutColors.current
}

@Composable
fun DebutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DebutDarkColorScheme else DebutLightColorScheme
    val extraColors = if (darkTheme) DebutDarkExtraColors else DebutLightExtraColors
    CompositionLocalProvider(LocalDebutColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = debutTypography(),
            shapes = DebutShapes,
            content = content,
        )
    }
}
