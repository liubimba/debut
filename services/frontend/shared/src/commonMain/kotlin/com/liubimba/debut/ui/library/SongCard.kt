package com.liubimba.debut.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Dot
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Trash2
import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.ui.format.formatDuration
import com.liubimba.debut.ui.format.formatNoteName
import com.liubimba.debut.ui.format.formatTakeDate
import com.liubimba.debut.ui.theme.PillShape
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.lastTake
import debut.shared.generated.resources.range
import debut.shared.generated.resources.song_tempo_value
import debut.shared.generated.resources.song_unknown_value
import debut.shared.generated.resources.tempo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt


@Composable
private fun Label(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DeleteSongCardButton(onDelete: () -> Unit, armedTimeoutMillis: Long = 3_000) {
    var armed by remember { mutableStateOf(false) }

    LaunchedEffect(armed) {
        if (armed) {
            delay(armedTimeoutMillis)
            armed = false
        }
    }

    TextButton(onClick = {
        if (armed) {
            armed = false
            onDelete()
        } else {
            armed = true
        }
    }) {
        if (armed) {
            Text(
                text = "Delete?",
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(16.dp)
                ).padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            Icon(
                imageVector = Lucide.Trash2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SongCard(
    song: SongMetadata,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val unknown = stringResource(Res.string.song_unknown_value)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(size = 16.dp))
            .background(color = MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .defaultMinSize(minWidth = 240.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Lucide.Music,
                contentDescription = null,
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = PillShape)
                    .padding(16.dp)
            )

            Column() {
                Text(text = song.name)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.author ?: unknown,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Icon(
                        imageVector = Lucide.Dot,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = song.durationSeconds?.let { formatDuration(it) } ?: unknown,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            DeleteSongCardButton(onDelete = onDelete)

            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }


        Box(
            modifier = Modifier.fillMaxWidth().height(2.dp)
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Label(
                label = stringResource(Res.string.tempo),
                text = song.tempoBpm?.let {
                    stringResource(Res.string.song_tempo_value, it.roundToInt())
                } ?: unknown
            )
            Label(
                label = stringResource(Res.string.range),
                text = song.range?.let {
                    "${formatNoteName(it.lowestMidi)}–${formatNoteName(it.highestMidi)}"
                } ?: unknown
            )
            Label(
                label = stringResource(Res.string.lastTake),
                text = song.lastTakeAt?.let { formatTakeDate(it) } ?: unknown
            )
        }
    }
}
