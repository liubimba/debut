package com.liubimba.debut.ui.song

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.VolumeX
import com.liubimba.debut.data.audio.Waveform

@Composable
fun TrackRow(
    label: String,
    waveform: Waveform,
    muted: Boolean,
    progress: () -> Float,
    onToggleMute: () -> Unit,
    onSeek: (Float) -> Unit,
    startArea: Float,
    endArea: Float,
    onSelectArea: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SongScreenDefaults.headerSpacing),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.weight(1f))

            IconButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (muted) Lucide.VolumeX else Lucide.Volume2,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(SongScreenDefaults.inlineIconSize),
                )
            }
        }

        WaveformView(
            waveform = waveform,
            progress = progress,
            onSeek = onSeek,
            startArea = startArea,
            endArea = endArea,
            onSelectedArea = onSelectArea,
            modifier = Modifier.fillMaxWidth().height(SongScreenDefaults.waveHeight),
        )
    }
}
