package com.liubimba.debut.data.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

actual class AudioInput actual constructor(
    sampleRate: Int,
    private val channels: Int,
    bufferFrames: Int,
) : AutoCloseable {
    private val format =
        AudioFormat(sampleRate.toFloat(), BITS_PER_SAMPLE, channels, true, false)

    private val line: TargetDataLine = AudioSystem.getTargetDataLine(format).apply {
        open(format, bufferFrames * channels * BYTES_PER_SAMPLE)
    }

    private var bytes = ByteArray(0)
    private var captured = 0L

    actual val framesCaptured: Long get() = captured

    actual fun start() = line.start()

    actual fun stop() {
        line.stop()
        line.flush()
    }

    actual fun read(destination: FloatArray, frames: Int): Int {
        val needed = frames * channels * BYTES_PER_SAMPLE
        if (bytes.size < needed) {
            bytes = ByteArray(needed)
        }
        val readBytes = line.read(bytes, 0, needed)
        val samples = readBytes / BYTES_PER_SAMPLE
        bytes.readPcm16(destination, samples)
        val readFrames = samples / channels
        captured += readFrames
        return readFrames
    }

    actual override fun close() {
        line.stop()
        line.close()
    }

    private companion object {
        const val BITS_PER_SAMPLE = 16
    }
}
