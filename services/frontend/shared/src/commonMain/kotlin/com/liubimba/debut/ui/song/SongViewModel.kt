package com.liubimba.debut.ui.song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.liubimba.debut.data.audio.SongAudio
import com.liubimba.debut.data.audio.Waveform
import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.data.entity.StemType
import com.liubimba.debut.data.repository.SongsRepository
import com.liubimba.debut.data.storage.SongsStorage
import com.liubimba.debut.data.storage.ioDispatcher
import com.liubimba.debut.data.storage.presentStems
import com.liubimba.debut.data.storage.waveformOf
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.song_no_stems
import debut.shared.generated.resources.song_playback_unavailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

data class SongTrack(
    val group: TrackGroup,
    val waveform: Waveform,
    val muted: Boolean,
)

sealed interface SongState {
    data object Loading : SongState

    data class Ready(
        val durationFrames: Long,
        val stemsTracks: List<SongTrack>,
    ) : SongState

    data class Unavailable(val reason: StringResource) : SongState
}

private data class LoadedSong(
    val durationFrames: Long,
    val stems: Map<TrackGroup, Waveform>,
)

class SongViewModel(
    private val songId: String,
    private val songsRepository: SongsRepository,
    private val songsStorage: SongsStorage,
    val microphoneGranted: StateFlow<Boolean>,
) : ViewModel() {
    private val log = Logger.withTag("SongViewModel")
    private val audioScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val audio = SongAudio(songId, songsStorage, audioScope)
    private val record = RecordController(
        songId = songId,
        storage = songsStorage,
        songs = songsRepository,
        audio = audio,
        microphoneGranted = microphoneGranted,
        scope = audioScope,
    )

    private val loaded = MutableStateFlow<LoadedSong?>(null)
    private val failure = MutableStateFlow<StringResource?>(null)

    val song: StateFlow<SongMetadata?> = songsRepository.songs
        .map { songs -> songs[songId] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val state: StateFlow<SongState> = combine(
        loaded,
        failure,
        audio.muted,
        record.takeWaveform,
    ) { song, unavailable, muted, take ->
        when {
            unavailable != null -> SongState.Unavailable(unavailable)
            song == null -> SongState.Loading
            else -> SongState.Ready(
                durationFrames = song.durationFrames,
                stemsTracks = song.stems.map { (group, waveform) ->
                    SongTrack(group, waveform, group.stems.all { it in muted })
                } + SongTrack(
                    group = TrackGroup.Recording,
                    waveform = take ?: Waveform.void(),
                    muted = StemType.RECORDING in muted,
                ),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SongState.Loading)

    val isPlaying = audio.isPlaying
    val selectedArea = audio.selectedArea
    val positionVersion = audio.positionVersion
    val isRecording = record.isRecording
    val microphoneError = record.microphoneError
    val requestMicrophone = record.requestMicrophone

    val positionFrames: Long get() = audio.positionFrames
    val sampleRate: Int get() = audio.format?.sampleRate ?: 1

    init {
        viewModelScope.launch { open() }
    }

    fun togglePlay() {
        audioScope.launch { audio.togglePlay() }
    }

    fun seekTo(frame: Long) {
        audioScope.launch { audio.seekTo(frame) }
    }

    fun selectArea(start: Float, end: Float) {
        audioScope.launch { audio.selectArea(start, end) }
    }

    fun toggleMute(group: TrackGroup) {
        audio.mute(group.stems, group.stems.none { it in audio.muted.value })
    }

    fun toggleRecord() = record.toggle()

    override fun onCleared() {
        record.cancel()
        audio.close()
        audioScope.cancel()
        super.onCleared()
    }

    private suspend fun open() {
        val present = songsStorage.presentStems(songId)
        if (present.isEmpty()) {
            failure.value = Res.string.song_no_stems
            log.w { "song $songId has no stems on disk" }
            return
        }
        try {
            audio.open(present)
        } catch (unopenable: Exception) {
            failure.value = Res.string.song_playback_unavailable
            log.e(unopenable) { "cannot open playback for $songId" }
            return
        }
        loaded.value = LoadedSong(
            durationFrames = audio.format?.frameCount ?: 0,
            stems = stemWaveforms(present),
        )
        record.restore()
    }

    private suspend fun stemWaveforms(present: List<StemType>): Map<TrackGroup, Waveform> {
        val perStem = present.associateWith { songsStorage.waveformOf(songId, it) }
        return TrackGroup.entries
            .associateWith { group -> group.stems.filter { it in present } }
            .filterValues { it.isNotEmpty() }
            .mapValues { (_, stems) -> Waveform.merge(stems.map { perStem.getValue(it) }) }
    }
}
