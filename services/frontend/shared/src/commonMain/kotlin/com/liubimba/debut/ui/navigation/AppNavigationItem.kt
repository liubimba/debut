package com.liubimba.debut.ui.navigation


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import com.liubimba.debut.ui.theme.DebutTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppNavigationItem(
    destination: Destination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = AppNavigationDefaults.barItemPadding,
) {
    val itemColor =
        if (isSelected) DebutTheme.colors.chassis else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(percent = AppNavigationDefaults.cornerPercent))
            .background(if (isSelected) DebutTheme.colors.onChassis else Color.Transparent)
            .clickable(onClick = onClick)
            .defaultMinSize(
                minWidth = AppNavigationDefaults.minWidth,
                minHeight = AppNavigationDefaults.minHeight
            )
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = destination.icon,
            tint = itemColor,
            contentDescription = stringResource(destination.titleRes)
        )

        Text(
            text = stringResource(destination.titleRes),
            style = MaterialTheme.typography.labelSmall,
            color = itemColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
