package com.liubimba.debut.ui.addsong

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.liubimba.debut.data.repository.SongsRepository
import com.liubimba.debut.ui.extensions.LocalWindowSize
import com.liubimba.debut.ui.extensions.WindowSize
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.add_song_back_title
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddSongScreen(
    songsRepository: SongsRepository,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSing: (String) -> Unit = {},
) {
    val viewModel: AddSongViewModel = viewModel { AddSongViewModel(songsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val launcher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = AUDIO_EXTENSIONS),
    ) { file ->
        file ?: return@rememberFilePickerLauncher
        scope.launch { viewModel.import(file.name, file.readBytes()) }
    }

    val compact = LocalWindowSize.current == WindowSize.Compact
    val screenPadding = if (compact) {
        AddSongDefaults.compactScreenPadding
    } else {
        AddSongDefaults.expandedScreenPadding
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(screenPadding),
            verticalArrangement = Arrangement.spacedBy(AddSongDefaults.contentSpacing),
        ) {
            AnimatedContent(targetState = state) { current ->
                when (current) {
                    is ImportState.Idle -> AddSongDropZone(onPick = launcher::launch)

                    is ImportState.InProgress -> AddSongInProgress(
                        stage = current.stage,
                        filename = current.filename,
                        elapsedSeconds = elapsedSeconds,
                        onCancel = viewModel::cancel,
                    )

                    is ImportState.Done -> AddSongDone(
                        song = current.song,
                        filename = current.filename,
                        elapsedSeconds = elapsedSeconds,
                        onSing = { onSing(current.song.id) },
                        onAddAnother = viewModel::reset,
                    )

                    is ImportState.Failed -> AddSongFailed(
                        reason = current.message,
                        onRetry = viewModel::reset,
                    )
                }
            }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    TextButton(
        onClick = onBack,
        modifier = Modifier.defaultMinSize(minHeight = AddSongDefaults.minTouchTarget),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AddSongDefaults.contentSpacing),
        ) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(AddSongDefaults.backIconPadding)
                    .size(AddSongDefaults.backIconSize),
            )
            Text(
                text = stringResource(Res.string.add_song_back_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val AUDIO_EXTENSIONS = listOf("mp3", "wav", "flac", "m4a", "ogg")
