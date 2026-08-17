package com.liubimba.debut.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.data.repository.SongsRepository
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.library_empty
import debut.shared.generated.resources.nav_library
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibraryScreen(
    songsRepository: SongsRepository,
    onAddSong: () -> Unit = {},
    onSelectSong: (song: SongMetadata) -> Unit = {}
) {
    val viewModel: LibraryViewModel = viewModel { LibraryViewModel(songsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(Res.string.nav_library),
            style = MaterialTheme.typography.headlineLarge
        )

        when (val current = state) {
            LibraryState.Loading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                CircularProgressIndicator()
            }

            LibraryState.Empty -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = stringResource(Res.string.library_empty))
                    AddSongButton(onClick = onAddSong)
                }
            }

            is LibraryState.Content -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = CARD_MIN_WIDTH),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items = current.songs, key = { song -> song.id }) { song ->
                    SongCard(
                        song = song,
                        onClick = { onSelectSong(song) },
                        onDelete = { viewModel.delete(song.id) }
                    )
                }

                item(key = ADD_SONG_KEY, span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AddSongButton(onClick = onAddSong)
                    }
                }
            }

            is LibraryState.Unavailable -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Text(text = current.message)
            }
        }


    }
}

private val CARD_MIN_WIDTH = 360.dp

private const val ADD_SONG_KEY = "add-song"
