package com.liubimba.debut.ui.song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.liubimba.debut.data.audio.MixerTrack
import com.liubimba.debut.data.audio.SelectedArea
import com.liubimba.debut.data.audio.SongPlayer
import com.liubimba.debut.data.audio.StemMixer
import com.liubimba.debut.data.audio.WavReader
import com.liubimba.debut.data.audio.Waveform
import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.data.entity.StemType
import com.liubimba.debut.data.repository.SongsRepository
import com.liubimba.debut.data.storage.SongsStorage
import com.liubimba.debut.data.storage.ioDispatcher
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.song_no_stems
import debut.shared.generated.resources.song_playback_unavailable
import org.jetbrains.compose.resources.StringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SongTrack(
    val group: TrackGroup,
    val waveform: Waveform,
    val muted: Boolean,
)


sealed interface SongState {
    data object Loading : SongState
    data class Ready(val tracks: List<SongTrack>, val durationFrames: Long) : SongState
    data class Unavailable(val reason: StringResource) : SongState

}

class SongViewModel(
    private val songId: String,
    private val songsRepository: SongsRepository,
    private val songsStorage: SongsStorage
) :
    ViewModel() {
    private val log = Logger.withTag("SongViewModel")
    private val playerScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    val song: StateFlow<SongMetadata?> = songsRepository.songs
        .map { songs -> songs[songId] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _state = MutableStateFlow<SongState>(SongState.Loading)
    val state = _state.asStateFlow()

    private val _isPlaying = MutableStateFlow<Boolean>(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _selectedArea = MutableStateFlow<SelectedArea>(SelectedArea())
    val selectedArea = _selectedArea.asStateFlow()

    private val _positionVersion = MutableStateFlow(0L)
    val positionVersion = _positionVersion.asStateFlow()

    private val _isRecording = MutableStateFlow<Boolean>(false)
    val isRecording = _isRecording.asStateFlow()

    private var player: SongPlayer? = null
    private val mixerTracks = mutableMapOf<StemType, MixerTrack>()


    val positionFrames: Long get() = player?.positionFrames ?: 0
    val sampleRate: Int get() = player?.format?.sampleRate ?: 1

    init {
        viewModelScope.launch {
            open()
        }
    }

    fun toggleRecord() {

    }

    fun togglePlay() {
        val current = player ?: return
        playerScope.launch {
            if (current.isPlaying.value) current.pause() else current.play()
        }
    }

    fun selectArea(start: Float, end: Float) {
        val current = player ?: return
        log.d { "Select $start $end" }
        playerScope.launch {
            current.selectArea(
                start = start,
                end = end
            )
        }
    }

    fun seekTo(frame: Long) {
        val current = player ?: return
        playerScope.launch {
            current.seekTo(frame)
        }
    }

    fun toggleMute(group: TrackGroup) {
        val ready = _state.value as? SongState.Ready ?: return
        val muted = !(ready.tracks.first { it.group == group }.muted)
        group.stems.forEach { type -> mixerTracks[type]?.muted = muted }
        _state.value = ready.copy(
            tracks = ready.tracks.map {
                if (it.group == group) it.copy(muted = muted) else it
            }
        )
    }

    override fun onCleared() {
        player?.close()
        playerScope.cancel()
        super.onCleared()
    }

    private suspend fun waveformOf(type: StemType): Waveform {
        songsStorage.waveforms.get(songId, type)?.let { return it }
        val computed = withContext(ioDispatcher) {
            WavReader(songsStorage.stems.pathOf(songId, type)).use { Waveform.of(it) }
        }
        songsStorage.waveforms.save(songId, type, computed)
        return computed
    }

    private suspend fun open() {
        val present = withContext(ioDispatcher) {
            StemType.entries.filter { songsStorage.stems.has(songId, it) }
        }
        if (present.isEmpty()) {
            _state.value = SongState.Unavailable(Res.string.song_no_stems)
            log.w { "song $songId has no stems on disk" }
            return
        }
        val waveforms = present.associateWith { waveformOf(it) }
        val groups = TrackGroup.entries
            .map { group -> group to group.stems.filter { it in present } }
            .filter { (_, stems) -> stems.isNotEmpty() }

        runCatching {
            withContext(ioDispatcher) {
                present.forEach { type ->
                    mixerTracks[type] =
                        MixerTrack(WavReader(songsStorage.stems.pathOf(songId, type)))
                }
                SongPlayer(
                    mixer = StemMixer(present.map { mixerTracks.getValue(it) }),
                    scope = playerScope
                )
            }
        }.onSuccess { opened ->
            player = opened
            playerScope.launch {
                opened.isPlaying.collect { _isPlaying.value = it }
            }
            playerScope.launch {
                opened.selectedArea.collect { _selectedArea.value = it }
            }
            playerScope.launch {
                opened.positionVersion.collect { _positionVersion.value = it }
            }
            _state.value = SongState.Ready(
                tracks = groups.map { (group, stems) ->
                    SongTrack(
                        group = group,
                        waveform = Waveform.merge(stems.map { waveforms.getValue(it) }),
                        muted = false,
                    )
                },
                durationFrames = opened.format.frameCount
            )
        }.onFailure { failure ->
            _state.value = SongState.Unavailable(Res.string.song_playback_unavailable)
            log.e(failure) { "cannot open playback for $songId" }
        }
    }
}
