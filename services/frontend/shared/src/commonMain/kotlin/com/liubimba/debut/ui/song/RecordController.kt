package com.liubimba.debut.ui.song

import co.touchlab.kermit.Logger
import com.liubimba.debut.data.audio.SongAudio
import com.liubimba.debut.data.audio.TakeRecorder
import com.liubimba.debut.data.audio.Waveform
import com.liubimba.debut.data.entity.TakeMetadata
import com.liubimba.debut.data.repository.SongsRepository
import com.liubimba.debut.data.storage.SongsStorage
import com.liubimba.debut.data.storage.lastTake
import com.liubimba.debut.data.storage.waveformOf
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.song_microphone_unavailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.StringResource
import kotlin.random.Random
import kotlin.time.Clock

class RecordController(
    private val songId: String,
    private val storage: SongsStorage,
    private val songs: SongsRepository,
    private val audio: SongAudio,
    private val microphoneGranted: StateFlow<Boolean>,
    private val scope: CoroutineScope,
) {
    private val log = Logger.withTag("RecordController")
    private val lock = Mutex()

    private var recorder: TakeRecorder? = null
    private var takeId: String? = null
    private var peaks: Job? = null
    private var liveBuckets = FloatArray(0)
    private var liveCursor = 0

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _takeWaveform = MutableStateFlow<Waveform?>(null)
    val takeWaveform = _takeWaveform.asStateFlow()

    private val _microphoneError = MutableStateFlow<StringResource?>(null)
    val microphoneError = _microphoneError.asStateFlow()

    private val _requestMicrophone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestMicrophone = _requestMicrophone.asSharedFlow()

    fun toggle() {
        scope.launch {
            lock.withLock {
                if (recorder == null) start() else finish()
            }
        }
    }

    suspend fun restore() {
        val last = storage.lastTake(songId) ?: return
        val format = audio.format ?: return
        try {
            audio.attachTake(last)
            _takeWaveform.value = storage.waveformOf(songId, last, format.frameCount)
            log.d { "restored take ${last.id} of $songId" }
        } catch (failure: Exception) {
            log.e(failure) { "unreadable take ${last.id} of $songId" }
        }
    }

    fun cancel() {
        peaks?.cancel()
        peaks = null
        recorder?.cancel()
        recorder = null
        _isRecording.value = false
    }

    private suspend fun start() {
        if (!microphoneGranted.value) {
            _requestMicrophone.tryEmit(Unit)
            return
        }
        val format = audio.format ?: return
        val startFrame = audio.positionFrames
        val id = newTakeId()
        val created = TakeRecorder(sampleRate = format.sampleRate, scope = scope)
        try {
            created.start(storage.takes.pathOf(songId, id), startFrame)
        } catch (failure: Exception) {
            _microphoneError.value = Res.string.song_microphone_unavailable
            log.e(failure) { "cannot start recording" }
            return
        }
        audio.detachTake()
        resetLive(startFrame, format.frameCount)
        peaks = scope.launch { created.peaks.collect { appendPeak(it) } }
        recorder = created
        takeId = id
        _microphoneError.value = null
        _isRecording.value = true
        audio.play()
        log.i { "take $id started at frame $startFrame" }
    }

    private suspend fun finish() {
        val current = recorder ?: return
        val id = takeId ?: return
        val take = current.stop()
        peaks?.cancel()
        peaks = null
        recorder = null
        takeId = null
        _isRecording.value = false
        audio.pause()
        if (take == null) {
            _takeWaveform.value = null
            log.i { "take $id discarded, nothing captured" }
            return
        }
        val saved = TakeMetadata(
            id = id,
            startFrame = take.startFrame,
            frameCount = take.frameCount,
        )
        storage.takes.save(songId = songId, take = saved)
        songs.markTake(songId, at = Clock.System.now())
        audio.attachTake(saved)
        audio.format?.let { _takeWaveform.value = storage.waveformOf(songId, saved, it.frameCount) }
        log.i { "take $id saved: ${take.frameCount} frames from ${take.startFrame}" }
    }

    private fun resetLive(startFrame: Long, songFrames: Long) {
        val bucket = Waveform.DEFAULT_FRAMES_PER_BUCKET
        liveBuckets = FloatArray(((songFrames + bucket - 1) / bucket).toInt()) { Waveform.VOID }
        liveCursor = (startFrame / bucket).toInt()
        publishLive()
    }

    private fun appendPeak(peak: Float) {
        if (liveCursor >= liveBuckets.size) {
            return
        }
        liveBuckets[liveCursor] = peak
        liveCursor += 1
        publishLive()
    }

    private fun publishLive() {
        _takeWaveform.value = Waveform(Waveform.DEFAULT_FRAMES_PER_BUCKET, liveBuckets.copyOf())
    }

    private fun newTakeId(): String =
        "${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(TAKE_ID_RANGE)}"

    private companion object {
        const val TAKE_ID_RANGE = 10_000
    }
}
