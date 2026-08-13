package com.liubimba.debut.ui.addsong

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Upload
import com.liubimba.debut.ui.extensions.dashedBorder
import com.liubimba.debut.ui.theme.PillShape
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.add_song_choose_button
import debut.shared.generated.resources.add_song_choose_tip
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddSongDropZone(onPick: () -> Unit, modifier: Modifier = Modifier) {
    AddSongCard(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AddSongDefaults.contentSpacing),
            modifier = Modifier
                .fillMaxWidth()
                .dashedBorder(
                    strokeWidth = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    cornerRadius = 16.dp,
                    dash = 4.dp,
                    gap = 4.dp,
                )
                .padding(
                    horizontal = AddSongDefaults.cardPadding,
                    vertical = AddSongDefaults.cardPadding * 2,
                ),
        ) {
            Icon(
                imageVector = Lucide.Upload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(AddSongDefaults.backIconSize),
            )
            Text(
                text = stringResource(Res.string.add_song_choose_tip),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onPick,
                modifier = Modifier.defaultMinSize(minHeight = AddSongDefaults.minTouchTarget),
                shape = PillShape,
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Text(
                    text = stringResource(Res.string.add_song_choose_button),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
