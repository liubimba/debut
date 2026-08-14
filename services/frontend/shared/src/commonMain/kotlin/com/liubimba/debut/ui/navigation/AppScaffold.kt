package com.liubimba.debut.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.liubimba.debut.ui.extensions.LocalWindowSize
import com.liubimba.debut.ui.extensions.WindowSize

@Composable
fun AppScaffold(
    current: Destination,
    onSelect: (Destination) -> Unit,
    immersive: Boolean = false,
    content: @Composable () -> Unit,
) {
    val expanded = LocalWindowSize.current == WindowSize.Expanded
    when {
        immersive -> content()

        expanded -> Row(Modifier.fillMaxSize()) {
            AppNavigationRail(current = current, onSelect = onSelect)
            Box(Modifier.weight(1f)) {
                content()
            }
        }

        else -> Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                content()
            }
            AppBottomBar(current = current, onSelect = onSelect)
        }
    }
}
