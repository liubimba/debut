package com.liubimba.debut.data.audio

import co.touchlab.kermit.Logger
import com.liubimba.debut.data.storage.ioDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlin.math.abs

data class Take(
    val path: Path,
    val startFrame: Long,
    val frameCount: Long
) {}

class TakeRecorder(
    private val sampleRate: Int,
    private val channels: Int = MONO,
    private val bufferFrames: Int = DEFAULT_BUFFER_FRAMES,
    private val framesPerBucket: Int = Waveform.DEFAULT_FRAMES_PER_BUCKET,
    private val scope: CoroutineScope
) {
    private val log = Logger.withTag("TakeRecorder")
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _peaks = MutableSharedFlow<Float>(
        extraBufferCapacity = PEAK_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val peaks = _peaks.asSharedFlow()

    private var input: AudioInput? = null
    private var writer: WavWriter? = null
    private var capture: Job? = null
    private var target: Path? = null
    private var startFrame = 0L
    val framesCaptured: Long get() = writer?.framesWritten ?: 0

    fun start(target: Path, fromFrame: Long) {
        if (_isRecording.value) {
            return
        }
        val opened = AudioInput(sampleRate, channels, bufferFrames)
        input = opened
        writer = WavWriter(target, sampleRate, channels)
        this.target = target
        startFrame = fromFrame
        _isRecording.value = true
        opened.start()
        capture = scope.launch(ioDispatcher) { captureLoop() }
    }

    suspend fun stop(): Take? {
        if (!_isRecording.value) {
            return null
        }
        _isRecording.value = false
        input?.stop()
        capture?.cancelAndJoin()
        capture = null
        val frames = writer?.framesWritten ?: 0
        release()

        val path = target ?: return null
        return if (frames > 0) Take(path, startFrame, frames) else null
    }

    fun cancel() {
        _isRecording.value = false
        input?.stop()
        capture?.cancel()
        capture = null
        release()
    }

    private fun release() {
        writer?.close()
        input?.close()
        writer = null
        input = null
    }

    private suspend fun captureLoop() {
        val source = input ?: return
        val sink = writer ?: return
        val buffer = FloatArray(bufferFrames * channels)
        var bucketFrames = 0
        var bucketPeak = 0f
        try {
            while (currentCoroutineContext().isActive) {
                val read = source.read(buffer, bufferFrames)
                if (read == 0) {
                    continue
                }
                sink.write(buffer, read)
                var taken = 0
                while (taken < read) {
                    val frames = minOf(read - taken, framesPerBucket - bucketFrames)
                    val peak = peakOf(buffer, taken * channels, (taken + frames) * channels)
                    if (peak > bucketPeak) {
                        bucketPeak = peak
                    }
                    taken += frames
                    bucketFrames += frames
                    if (bucketFrames == framesPerBucket) {
                        _peaks.tryEmit(bucketPeak)
                        bucketFrames = 0
                        bucketPeak = 0f
                    }
                }
            }
        } catch (failure: Exception) {
            if (currentCoroutineContext().isActive) {
                throw failure
            }
        }
    }

    private fun peakOf(buffer: FloatArray, from: Int, to: Int): Float {
        var peak = 0f
        for (sample in from until to) {
            val value = abs(buffer[sample])
            if (value > peak) {
                peak = value
            }
        }
        return peak
    }

    private companion object {
        const val MONO = 1
        const val DEFAULT_BUFFER_FRAMES = 2048
        const val PEAK_BUFFER = 64
    }
}
