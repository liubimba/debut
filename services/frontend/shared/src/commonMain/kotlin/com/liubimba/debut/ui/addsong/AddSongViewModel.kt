package com.liubimba.debut.ui.addsong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liubimba.debut.data.repository.ImportStage
import com.liubimba.debut.data.repository.ImportedSong
import com.liubimba.debut.data.repository.SongsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface ImportState {
    data object Idle : ImportState

    data class InProgress(val stage: ImportStage, val filename: String) : ImportState

    data class Done(val song: ImportedSong, val filename: String) : ImportState

    data class Failed(val message: String, val filename: String) : ImportState
}

class AddSongViewModel(private val repository: SongsRepository) : ViewModel() {
    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private var importJob: Job? = null

    fun import(name: String, bytes: ByteArray) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _elapsedSeconds.value = 0
            _state.value = ImportState.InProgress(ImportStage.UPLOADING, name)
            val ticker = launch {
                while (isActive) {
                    delay(TICK_MILLIS)
                    _elapsedSeconds.value += 1
                }
            }
            try {
                val song = repository.import(name, bytes) { stage ->
                    _state.value = ImportState.InProgress(stage, name)
                }
                _state.value = ImportState.Done(song, name)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _state.value = ImportState.Failed(failure.message ?: DEFAULT_ERROR, name)
            } finally {
                ticker.cancel()
            }
        }
    }

    fun cancel() {
        importJob?.cancel()
        importJob = null
        reset()
    }

    fun reset() {
        _state.value = ImportState.Idle
        _elapsedSeconds.value = 0
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val DEFAULT_ERROR = "import failed"
    }
}
