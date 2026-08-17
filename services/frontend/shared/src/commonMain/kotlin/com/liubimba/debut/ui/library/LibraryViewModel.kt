package com.liubimba.debut.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.data.repository.SongsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


sealed interface LibraryState {
    data object Loading : LibraryState
    data object Empty : LibraryState
    data class Content(val songs: List<SongMetadata>) : LibraryState
    data class Unavailable(val message: String) : LibraryState
}

class LibraryViewModel(private val repository: SongsRepository) : ViewModel() {
    private val log = Logger.withTag(TAG)

    val state: StateFlow<LibraryState> = repository.songs
        .map { songs ->
            if (songs.isEmpty()) LibraryState.Empty else LibraryState.Content(
                ArrayList(
                    songs.values
                )
            )
        }
        .onEach { current ->
            log.d {
                when (current) {
                    is LibraryState.Content -> "library shows ${current.songs.size} songs"
                    is LibraryState.Unavailable -> "library unavailable: ${current.message}"
                    else -> "library is ${current::class.simpleName}"
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState.Loading)

    override fun onCleared() {
        log.d { "cleared" }
        super.onCleared()
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching {
                repository.delete(id)
            }.onFailure {
                log.e(it) { "failed to delete $id" }
            }
        }
    }

    private companion object {
        const val TAG = "LibraryViewModel"
    }
}
