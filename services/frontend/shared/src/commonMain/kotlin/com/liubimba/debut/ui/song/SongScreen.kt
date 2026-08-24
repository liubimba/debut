package com.liubimba.debut.ui.song

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.data.repository.SongsRepository
import com.liubimba.debut.data.storage.SongsStorage
import com.liubimba.debut.ui.format.formatDuration
import com.liubimba.debut.ui.format.formatNoteName
import com.liubimba.debut.ui.theme.PillShape
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.length
import debut.shared.generated.resources.nav_library
import debut.shared.generated.resources.range
import debut.shared.generated.resources.song_tempo_value
import debut.shared.generated.resources.song_unknown_value
import debut.shared.generated.resources.tempo
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun SongScreen(
    id: String,
    songsRepository: SongsRepository,
    songsStorage: SongsStorage,
    onBack: () -> Unit = {},
) {
    val viewModel: SongViewModel =
        viewModel { SongViewModel(id, songsRepository, songsStorage) }
    val song by viewModel.song.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()

    var viewType by remember { mutableStateOf(VocalViewType.VocalTrack) }
    val position = remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            withInfiniteAnimationFrameNanos { }
            position.longValue = viewModel.positionFrames
        }
        position.longValue = viewModel.positionFrames
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(SongScreenDefaults.blockSpacing),
    ) {
        BackToLibrary(onBack = onBack)

        SongHeader(song = song)

        when (val current = state) {
            is SongState.Ready -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(SongScreenDefaults.headerSpacing),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    VocalViewType.entries.forEach { type ->
                        VocalTypeButton(
                            icon = type.icon,
                            text = stringResource(type.titleRes),
                            active = viewType == type,
                            enabled = type.implemented,
                            onClick = { viewType = type },
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(SongScreenDefaults.trackSpacing),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(
                            elevation = SongScreenDefaults.panelElevation,
                            shape = MaterialTheme.shapes.extraLarge,
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.extraLarge,
                        )
                        .padding(SongScreenDefaults.panelInset)
                        .verticalScroll(rememberScrollState()),
                ) {
                    current.tracks.forEachIndexed { index, track ->
                        TrackRow(
                            label = stringResource(track.group.titleRes),
                            waveform = track.waveform,
                            muted = track.muted,
                            progress = { position.longValue.toFloat() / current.durationFrames },
                            onToggleMute = { viewModel.toggleMute(track.group) },
                            onSeek = { fraction ->
                                position.longValue = (fraction * current.durationFrames).toLong()
                                viewModel.seekTo(position.longValue)
                            },
                        )
                        if (index < current.tracks.lastIndex) {
                            TrackDivider()
                        }
                    }
                }

                TransportBar(
                    isPlaying = isPlaying,
                    isRecording = isRecording,
                    positionSeconds = { position.longValue.toDouble() / viewModel.sampleRate },
                    durationSeconds = current.durationFrames.toDouble() / viewModel.sampleRate,
                    onToggleRecord = viewModel::toggleRecord,
                    onTogglePlay = viewModel::togglePlay,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            SongState.Loading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                CircularProgressIndicator()
            }

            is SongState.Unavailable -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                Text(text = current.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun BackToLibrary(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SongScreenDefaults.headerSpacing),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onBack)
            .defaultMinSize(minHeight = SongScreenDefaults.minTouchTarget)
            .padding(vertical = SongScreenDefaults.backIconPadding),
    ) {
        Icon(
            imageVector = Lucide.ArrowLeft,
            contentDescription = null,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, PillShape)
                .padding(SongScreenDefaults.backIconPadding)
                .size(SongScreenDefaults.inlineIconSize),
        )
        Text(text = stringResource(Res.string.nav_library))
    }
}

@Composable
private fun SongHeader(song: SongMetadata?) {
    val unknown = stringResource(Res.string.song_unknown_value)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(SongScreenDefaults.metaSpacing),
        verticalArrangement = Arrangement.spacedBy(SongScreenDefaults.blockSpacing),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Text(
                text = song?.name ?: unknown,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = song?.author ?: unknown,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(SongScreenDefaults.metaSpacing)) {
            MetaTitle(
                title = stringResource(Res.string.tempo),
                text = song?.tempoBpm
                    ?.let { stringResource(Res.string.song_tempo_value, it.roundToInt()) }
                    ?: unknown,
            )
            MetaTitle(
                title = stringResource(Res.string.range),
                text = song?.range
                    ?.let { "${formatNoteName(it.lowestMidi)}–${formatNoteName(it.highestMidi)}" }
                    ?: unknown,
            )
            MetaTitle(
                title = stringResource(Res.string.length),
                text = song?.durationSeconds?.let { formatDuration(it) } ?: unknown,
            )
        }
    }
}

@Composable
private fun TrackDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(SongScreenDefaults.dividerHeight)
            .background(
                color = MaterialTheme.colorScheme.outline
                    .copy(alpha = SongScreenDefaults.dividerAlpha)
            )
    )
}

@Composable
private fun VocalTypeButton(
    icon: ImageVector,
    text: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            },
            contentColor = if (active) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
        modifier = Modifier.defaultMinSize(minHeight = SongScreenDefaults.minTouchTarget),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(SongScreenDefaults.inlineIconSize),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MetaTitle(title: String, text: String) {
    Column {
        Text(
            text = title.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
