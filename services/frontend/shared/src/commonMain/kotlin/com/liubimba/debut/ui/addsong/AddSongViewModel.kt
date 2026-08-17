package com.liubimba.debut.ui.addsong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.data.repository.ImportStage
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

    data class Done(val song: SongMetadata, val filename: String) : ImportState

    data class Failed(val message: String, val filename: String) : ImportState
}

class AddSongViewModel(private val repository: SongsRepository) : ViewModel() {
    private val log = Logger.withTag(TAG)
    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private var importJob: Job? = null

    fun import(name: String, bytes: ByteArray) {
        log.i { "import $name" }
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
                log.i { "imported $name" }
                _state.value = ImportState.Done(song, name)
            } catch (cancellation: CancellationException) {
                log.w { "import $name has been cancelled" }
                throw cancellation
            } catch (failure: Exception) {
                log.e(throwable = failure) { "import $name failed" }
                _state.value = ImportState.Failed(failure.message ?: DEFAULT_ERROR, name)
            } finally {
                ticker.cancel()
            }
        }
    }

    fun cancel() {
        log.i { "cancelling import" }
        importJob?.cancel()
        importJob = null
        reset()
    }

    fun reset() {
        log.d { "reset from ${_state.value}" }
        _state.value = ImportState.Idle
        _elapsedSeconds.value = 0
    }

    override fun onCleared() {
        log.d { "cleared" }
        super.onCleared()
    }

    private companion object {
        const val TAG = "AddSongViewModel"
        const val TICK_MILLIS = 1_000L
        const val DEFAULT_ERROR = "import failed"
    }
}
