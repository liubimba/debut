package com.liubimba.debut.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

actual class AudioOutput actual constructor(
    sampleRate: Int,
    private val channels: Int,
    bufferFrames: Int
) : AutoCloseable {
    private val channelMask =
        if (channels == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO

    private val track: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(channelMask)
                .build()
        )
        .setBufferSizeInBytes(
            maxOf(
                bufferFrames * channels * BYTES_PER_SAMPLE,
                AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
            )
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

    private var bytes = ByteArray(0)
    private var flushedFrames: Long = 0

    actual val framesPlayed: Long
        get() = flushedFrames + (track.playbackHeadPosition.toLong() and 0xFFFFFFFFL)

    actual fun play() = track.play()

    actual fun pause() = track.pause()
    actual fun write(samples: FloatArray, frames: Int) {
        val count = frames * channels
        val needed = count * BYTES_PER_SAMPLE
        if (bytes.size < needed) {
            bytes = ByteArray(needed)
        }
        samples.writePcm16(bytes, count)
        track.write(bytes, 0, needed)
    }

    actual fun flush() {
        track.pause()
        flushedFrames = framesPlayed
        track.flush()
    }

    actual override fun close() {
        track.stop()
        track.release()
    }

}
