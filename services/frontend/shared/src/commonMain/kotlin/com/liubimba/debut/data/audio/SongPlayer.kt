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

data class SelectedArea(
    val start: Float = 0f,
    val end: Float = 1f,
) {
    val isFull: Boolean get() = start <= 0f && end >= 1f

    companion object {
        val Full = SelectedArea()

        fun of(start: Float, end: Float): SelectedArea = SelectedArea(
            start = minOf(start, end).coerceIn(0f, 1f),
            end = maxOf(start, end).coerceIn(0f, 1f),
        )
    }
}

class SongPlayer(
    private val mixer: StemMixer,
    private val scope: CoroutineScope,
    private val bufferFrames: Int = DEFAULT_BUFFER_FRAMES
) : AutoCloseable {
    private val log = Logger.withTag("SongPlayer")

    private val output = AudioOutput(mixer.format.sampleRate, mixer.format.channels, bufferFrames)
    private val buffer = FloatArray(bufferFrames * mixer.format.channels)

    private val _selectedArea = MutableStateFlow<SelectedArea>(SelectedArea())
    val selectedArea = _selectedArea.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished = _finished.asStateFlow()

    private val _positionVersion = MutableStateFlow(0L)
    val positionVersion = _positionVersion.asStateFlow()

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
        if (_finished.value || positionFrames >= limitFrame()) {
            log.d { "Requested exhausted player to play. Seek to start frame ${startFrame()}" }
            seekTo(startFrame())
        }
        _isPlaying.value = true
        _finished.value = false
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
        _finished.value = false
        seekOrigin = target
        framesPlayedAtSeek = output.framesPlayed
        _positionVersion.value++

        if (frame > limitFrame() || frame < startFrame()) {
            selectArea(SelectedArea.Full.start, SelectedArea.Full.end)
        }
        if (resume) {
            play()
        }
    }

    suspend fun selectArea(start: Float, end: Float) {
        _selectedArea.value = SelectedArea.of(start, end)
        if (!_isPlaying.value) {
            if (positionFrames < startFrame() || positionFrames > limitFrame()) {
                seekTo(startFrame())
            }
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

    private fun startFrame(): Long = (selectedArea.value.start * format.frameCount).toLong()

    private fun limitFrame(): Long = (selectedArea.value.end * format.frameCount).toLong()

    private suspend fun feedLoop() {
        var fed = seekOrigin
        try {
            while (currentCoroutineContext().isActive) {
                if (!_isPlaying.value) {
                    _isPlaying.first { it }
                    continue
                }

                val room = (limitFrame() - fed).coerceAtMost(bufferFrames.toLong()).toInt()
                if (room <= 0) {
                    log.i { "Out of room. Break feed loop" }
                    finish()
                    return
                }
                val produced = mixer.mix(buffer, room)
                if (produced == 0) {
                    log.i { "Mixer exhausted. Break feed loop" }
                    finish()
                    return
                }
                output.write(buffer, produced)
                fed += produced
            }
        } catch (failure: Exception) {
            if (currentCoroutineContext().isActive) {
                throw failure
            }
        }
    }

    private fun finish() {
        output.drain()
        _isPlaying.value = false
        _finished.value = true
        output.pause()
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
