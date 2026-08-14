package com.liubimba.debut.ui.extensions

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.liubimba.debut.ui.navigation.AppNavigationDefaults

enum class WindowSize {
    Compact, Expanded
}

val LocalWindowSize = compositionLocalOf { WindowSize.Compact }

@Composable
fun AppProvider(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val windowSize =
            if (maxWidth >= AppNavigationDefaults.expandedThreshold) WindowSize.Expanded else WindowSize.Compact
        CompositionLocalProvider(LocalWindowSize provides windowSize) {
            content()
        }
    }
}
