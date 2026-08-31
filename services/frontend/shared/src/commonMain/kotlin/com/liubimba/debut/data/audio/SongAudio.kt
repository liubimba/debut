package com.liubimba.debut.data.audio

import co.touchlab.kermit.Logger
import com.liubimba.debut.data.entity.StemType
import com.liubimba.debut.data.entity.TakeMetadata
import com.liubimba.debut.data.storage.SongsStorage
import com.liubimba.debut.data.storage.ioDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class SongAudio(
    private val songId: String,
    private val storage: SongsStorage,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val log = Logger.withTag("SongAudio")
    private val tracks = mutableMapOf<StemType, MixerTrack>()
    private var mixer: StemMixer? = null

    private val _player = MutableStateFlow<SongPlayer?>(null)

    private val _muted = MutableStateFlow<Set<StemType>>(emptySet())
    val muted = _muted.asStateFlow()

    val isPlaying: StateFlow<Boolean> = _player
        .flatMapLatest { it?.isPlaying ?: flowOf(false) }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val selectedArea: StateFlow<SelectedArea> = _player
        .flatMapLatest { it?.selectedArea ?: flowOf(SelectedArea.Full) }
        .stateIn(scope, SharingStarted.Eagerly, SelectedArea.Full)

    val positionVersion: StateFlow<Long> = _player
        .flatMapLatest { it?.positionVersion ?: flowOf(0L) }
        .stateIn(scope, SharingStarted.Eagerly, 0L)

    val format: WavFormat? get() = _player.value?.format
    val positionFrames: Long get() = _player.value?.positionFrames ?: 0

    suspend fun open(stems: List<StemType>) {
        require(stems.isNotEmpty()) { "song $songId has no stems" }
        withContext(ioDispatcher) {
            stems.forEach { type ->
                tracks[type] = MixerTrack(WavReader(storage.stems.pathOf(songId, type)))
            }
            val opened = StemMixer(stems.map { tracks.getValue(it) })
            mixer = opened
            _player.value = SongPlayer(mixer = opened, scope = scope)
        }
        log.i { "opened ${stems.size} stems of $songId" }
    }

    suspend fun attachTake(take: TakeMetadata) {
        val player = _player.value ?: return
        val mixer = mixer ?: return
        detachTake()
        val track = withContext(ioDispatcher) {
            MixerTrack(
                TakeSource(
                    take = WavReader(storage.takes.pathOf(songId, take.id)),
                    startFrame = take.startFrame,
                    songFrames = player.format.frameCount,
                    channels = player.format.channels,
                ),
                muted = StemType.RECORDING in _muted.value,
            )
        }
        mixer.attach(track)
        tracks[StemType.RECORDING] = track
        player.seekTo(player.positionFrames)
        log.d { "take ${take.id} attached at frame ${take.startFrame}" }
    }

    fun detachTake() {
        val track = tracks.remove(StemType.RECORDING) ?: return
        mixer?.detach(track)
        track.reader.close()
    }

    fun mute(stems: List<StemType>, muted: Boolean) {
        stems.forEach { tracks[it]?.muted = muted }
        _muted.value = if (muted) _muted.value + stems else _muted.value - stems.toSet()
    }

    suspend fun togglePlay() {
        val player = _player.value ?: return
        if (player.isPlaying.value) player.pause() else player.play()
    }

    suspend fun play() {
        _player.value?.play()
    }

    fun pause() {
        _player.value?.pause()
    }

    suspend fun seekTo(frame: Long) {
        _player.value?.seekTo(frame)
    }

    suspend fun selectArea(start: Float, end: Float) {
        _player.value?.selectArea(start, end)
    }

    override fun close() {
        _player.value?.close()
        _player.value = null
        tracks.clear()
        mixer = null
    }
}
