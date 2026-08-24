package com.liubimba.debut.data.audio

import co.touchlab.kermit.Logger
import com.liubimba.debut.data.storage.ioDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SongPlayer(
    private val mixer: StemMixer,
    private val scope: CoroutineScope,
    private val bufferFrames: Int = DEFAULT_BUFFER_FRAMES
) : AutoCloseable {
    private val log = Logger.withTag("SongPlayer")

    private val output = AudioOutput(mixer.format.sampleRate, mixer.format.channels, bufferFrames)
    private val buffer = FloatArray(bufferFrames * mixer.format.channels)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    private var feed: Job? = null
    private var seekOrigin = 0L
    private var framesPlayedAtSeek = 0L

    val format: WavFormat = mixer.format
    val positionFrames: Long
        get() = songFrame(seekOrigin, framesPlayedAtSeek, output.framesPlayed)
            .coerceIn(0, format.frameCount)

    suspend fun play() {
        if (_isPlaying.value) {
            return
        }
        if (positionFrames >= format.frameCount) {
            seekTo(0L)
        }
        _isPlaying.value = true
        output.play()
        if (feed?.isActive != true) {
            feed = scope.launch(ioDispatcher) {
                feedLoop()
            }
        }
    }

    fun pause() {
        if (!_isPlaying.value) {
            return
        }
        _isPlaying.value = false
        output.pause()

    }

    suspend fun seekTo(frame: Long) {
        val resume = _isPlaying.value
        stopFeeding()
        val target = frame.coerceIn(0, format.frameCount)
        mixer.seekTo(target)
        seekOrigin = target
        framesPlayedAtSeek = output.framesPlayed
        if (resume) {
            play()
        }
    }

    override fun close() {
        feed?.cancel()
        output.flush()
        output.close()
        mixer.close()
    }

    private suspend fun stopFeeding() {
        _isPlaying.value = false
        output.pause()
        output.flush()
        feed?.cancelAndJoin()
        feed = null
    }

    private suspend fun feedLoop() {
        try {
            while (currentCoroutineContext().isActive) {
                if (!_isPlaying.value) {
                    _isPlaying.first { it }
                    continue
                }
                val produced = mixer.mix(buffer, bufferFrames)
                if (produced == 0) {
                    _isPlaying.value = false
                    output.pause()
                    return
                }
                output.write(buffer, produced)
            }
        } catch (failure: Exception) {
            if (currentCoroutineContext().isActive) {
                throw failure
            }
        }
    }

    private companion object {
        const val DEFAULT_BUFFER_FRAMES = 2048
    }
}

internal fun songFrame(
    seekOrigin: Long,
    framesPlayedAtSeek: Long,
    framesPlayed: Long,
): Long = seekOrigin + (framesPlayed - framesPlayedAtSeek)
