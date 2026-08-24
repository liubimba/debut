package com.liubimba.debut.data.audio


expect class AudioOutput(
    sampleRate: Int, channels: Int, bufferFrames: Int
) : AutoCloseable {
    val framesPlayed: Long
    fun play()
    fun pause()
    fun write(samples: FloatArray, frames: Int)
    fun flush()
    override fun close()
}

fun FloatArray.writePcm16(destination: ByteArray, samples: Int) {
    var cursor = 0
    for (sample in 0 until samples) {
        val value = (this[sample].coerceIn(-1f, 1f) * PCM16_MAX).toInt()
        destination[cursor++] = (value and 0xFF).toByte()
        destination[cursor++] = ((value shr 8) and 0xFF).toByte()
    }
}

const val BYTES_PER_SAMPLE = 2
private const val PCM16_MAX = 32767f
