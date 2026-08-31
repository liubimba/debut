package com.liubimba.debut.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission


@SuppressLint("MissingPermission")
actual class AudioInput @RequiresPermission(Manifest.permission.RECORD_AUDIO) actual constructor(
    sampleRate: Int,
    private val channels: Int,
    bufferFrames: Int,
) : AutoCloseable {

    private val channelMask =
        if (channels == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO

    private val record: AudioRecord = try {
        AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
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
                    AudioRecord.getMinBufferSize(
                        sampleRate,
                        channelMask,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                )
            )
            .build()
    } catch (failure: UnsupportedOperationException) {
        throw MicrophoneUnavailable("RECORD_AUDIO permission is not granted")
    } catch (failure: SecurityException) {
        throw MicrophoneUnavailable("RECORD_AUDIO permission is not granted")
    }

    private var bytes = ByteArray(0)
    private var captured = 0L

    init {
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw MicrophoneUnavailable("microphone is busy or unsupported")
        }
    }

    actual val framesCaptured: Long get() = captured

    actual fun start() = record.startRecording()
    actual fun stop() = record.stop()
    actual fun read(destination: FloatArray, frames: Int): Int {
        val needed = frames * channels * BYTES_PER_SAMPLE
        if (bytes.size < needed) {
            bytes = ByteArray(needed)
        }
        val readBytes = record.read(bytes, 0, needed)
        if (readBytes <= 0) {
            return 0
        }
        val samples = readBytes / BYTES_PER_SAMPLE
        bytes.readPcm16(destination, samples)
        val readFrames = samples / channels
        captured += readFrames
        return readFrames
    }

    actual override fun close() {
        record.stop()
        record.release()
    }
}
