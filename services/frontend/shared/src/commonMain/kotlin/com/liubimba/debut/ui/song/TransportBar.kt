package com.liubimba.debut.ui.song

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.draw.shadow
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Square
import com.liubimba.debut.ui.format.formatDuration
import com.liubimba.debut.ui.theme.DebutTheme
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.pause
import debut.shared.generated.resources.play
import debut.shared.generated.resources.record
import debut.shared.generated.resources.stop
import org.jetbrains.compose.resources.stringResource

@Composable
fun TransportBar(
    isPlaying: Boolean,
    isRecording: Boolean,
    positionSeconds: () -> Double,
    durationSeconds: Double,
    onTogglePlay: () -> Unit,
    onToggleRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SongScreenDefaults.blockSpacing),
        modifier = modifier
            .shadow(
                elevation = SongScreenDefaults.dockElevation,
                shape = MaterialTheme.shapes.large,
            )
            .background(
                color = DebutTheme.colors.chassis,
                shape = MaterialTheme.shapes.large,
            )
            .padding(
                horizontal = SongScreenDefaults.blockSpacing,
                vertical = SongScreenDefaults.headerSpacing,
            ),
    ) {
        Row {
            Text(
                text = formatDuration(positionSeconds()),
                style = MaterialTheme.typography.titleMedium,
                color = DebutTheme.colors.onChassis,
            )
            Text(
                text = " / ${formatDuration(durationSeconds)}",
                style = MaterialTheme.typography.titleMedium,
                color = DebutTheme.colors.onChassis.copy(alpha = MUTED_ALPHA),
            )
        }

        Button(
            onClick = onToggleRecord,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.defaultMinSize(minHeight = SongScreenDefaults.minTouchTarget),
        ) {
            Icon(
                imageVector = if (isRecording) Lucide.Square else Lucide.Circle,
                contentDescription = null,
                modifier = Modifier.size(SongScreenDefaults.inlineIconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(if (isRecording) Res.string.stop else Res.string.record))
        }

        Button(
            onClick = onTogglePlay,
            colors = ButtonDefaults.buttonColors(
                containerColor = DebutTheme.colors.signal,
                contentColor = DebutTheme.colors.onSignal,
            ),
            modifier = Modifier.defaultMinSize(minHeight = SongScreenDefaults.minTouchTarget),
        ) {
            Icon(
                imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                contentDescription = null,
                modifier = Modifier.size(SongScreenDefaults.inlineIconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(if (isPlaying) Res.string.pause else Res.string.play))
        }
    }
}

private const val MUTED_ALPHA = 0.6f
