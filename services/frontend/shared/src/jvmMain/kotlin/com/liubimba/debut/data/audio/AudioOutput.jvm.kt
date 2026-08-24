package com.liubimba.debut.data.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

actual class AudioOutput actual constructor(
    sampleRate: Int,
    private val channels: Int,
    bufferFrames: Int
) : AutoCloseable {
    private val format =
        AudioFormat(sampleRate.toFloat(), BITS_PER_SAMPLE, channels, true, false)

    private val line: SourceDataLine = AudioSystem.getSourceDataLine(format).apply {
        open(format, bufferFrames * channels * BYTES_PER_SAMPLE)
    }

    private var bytes = ByteArray(0)

    actual override fun close() {
        line.stop()
        line.close()
    }

    actual val framesPlayed: Long
        get() = line.longFramePosition

    actual fun play() = line.start()

    actual fun pause() = line.stop()

    actual fun write(samples: FloatArray, frames: Int) {
        val count = frames * channels
        val needed = count * BYTES_PER_SAMPLE
        if (bytes.size < needed) {
            bytes = ByteArray(needed)
        }
        samples.writePcm16(bytes, count)
        line.write(bytes, 0, needed)
    }

    actual fun flush() = line.flush()

    private companion object {
        const val BITS_PER_SAMPLE = 16
    }
}
