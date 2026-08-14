package com.liubimba.debut.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.liubimba.debut.ui.theme.DebutTheme

@Composable
fun AppBottomBar(current: Destination, onSelect: (Destination) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppNavigationDefaults.barGap),
        modifier = Modifier
            .padding(AppNavigationDefaults.barMargin)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .height(AppNavigationDefaults.barHeight)
            .background(color = DebutTheme.colors.chassis)
            .padding(AppNavigationDefaults.barPadding)
    ) {
        Destination.entries.forEach { destination ->
            AppNavigationItem(
                destination = destination,
                isSelected = destination == current,
                onClick = { onSelect(destination) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
