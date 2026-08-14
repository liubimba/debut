package com.liubimba.debut.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.liubimba.debut.ui.theme.DebutTheme

@Composable
fun AppNavigationRail(
    current: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppNavigationDefaults.railGap),
        modifier = modifier
            .padding(AppNavigationDefaults.railMargin)
            .fillMaxHeight()
            .width(AppNavigationDefaults.railWidth)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(color = DebutTheme.colors.chassis)
            .padding(AppNavigationDefaults.railPadding),
    ) {
        for (destination in Destination.entries.filterNot { it.pinnedToEnd }) {
            AppNavigationItem(
                destination = destination,
                isSelected = destination == current,
                onClick = { onSelect(destination) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = AppNavigationDefaults.railItemPadding,
            )
        }

        Spacer(Modifier.weight(1f))

        for (destination in Destination.entries.filter { it.pinnedToEnd }) {
            AppNavigationItem(
                destination = destination,
                isSelected = destination == current,
                onClick = { onSelect(destination) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = AppNavigationDefaults.railItemPadding,
            )
        }

        AppWordmark()
    }
}
