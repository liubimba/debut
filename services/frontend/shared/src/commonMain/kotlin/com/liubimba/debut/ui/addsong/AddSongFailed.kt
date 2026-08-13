package com.liubimba.debut.ui.addsong

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
import com.liubimba.debut.ui.theme.DebutTheme
import com.liubimba.debut.ui.theme.PillShape
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.add_song_failed_title
import debut.shared.generated.resources.add_song_retry
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddSongFailed(
    reason: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AddSongCard(modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AddSongDefaults.inlineSpacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Lucide.TriangleAlert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(AddSongDefaults.backIconSize),
            )
            Text(
                text = stringResource(Res.string.add_song_failed_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium,
            color = DebutTheme.colors.errorText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onRetry,
            modifier = Modifier.defaultMinSize(minHeight = AddSongDefaults.minTouchTarget),
            shape = PillShape,
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        ) {
            Text(
                text = stringResource(Res.string.add_song_retry),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
