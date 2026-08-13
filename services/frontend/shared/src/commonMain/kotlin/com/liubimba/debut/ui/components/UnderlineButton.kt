package com.liubimba.debut.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun UnderlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minTouchTarget: Dp = 44.dp,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = minTouchTarget),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = TextDecoration.Underline,
        )
    }
}
