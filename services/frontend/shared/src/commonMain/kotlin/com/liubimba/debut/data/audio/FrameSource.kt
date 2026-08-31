package com.liubimba.debut.data.audio

interface FrameSource : AutoCloseable {
    val format: WavFormat
    fun seekTo(frame: Long)
    fun readFrames(destination: FloatArray, frames: Int): Int
}
