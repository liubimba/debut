package com.liubimba.debut.data.audio

expect class AudioInput(sampleRate: Int, channels: Int, bufferFrames: Int) : AutoCloseable {
    val framesCaptured: Long

    fun start()
    fun stop()
    fun read(destination: FloatArray, frames: Int): Int
    override fun close()
}

fun ByteArray.readPcm16(destination: FloatArray, samples: Int) {
    var cursor = 0
    for (sample in 0 until samples) {
        val low = this[cursor++].toInt() and 0xFF
        val high = this[cursor++].toInt()
        destination[sample] = ((high shl 8) or low) / PCM16_MAX
    }
}
