package com.liubimba.debut.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music4
import com.liubimba.debut.ui.theme.DebutTheme
import com.liubimba.debut.ui.theme.PillShape

@Composable
fun AppWordmark() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio = 1f)
            .clip(PillShape)
            .background(color = MaterialTheme.colorScheme.primary)
    ) {
        Icon(
            imageVector = Lucide.Music4,
            contentDescription = null,
            tint = DebutTheme.colors.chassis
        )
    }
}
