package com.liubimba.debut.data.audio

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeIntLe
import kotlinx.io.writeShortLe
import kotlinx.io.writeString

class WavWriter(
    private val target: Path,
    private val sampleRate: Int,
    private val channels: Int
) : AutoCloseable {
    private val temp = Path("$target.pcm")
    private val pcm = run {
        target.parent?.let { SystemFileSystem.createDirectories(it) }
        SystemFileSystem.sink(temp).buffered()
    }
    private var bytes = ByteArray(0)

    var framesWritten: Long = 0
        private set

    fun write(samples: FloatArray, frames: Int) {
        val count = frames * channels
        val needed = count * BYTES_PER_SAMPLE
        if (bytes.size < needed) {
            bytes = ByteArray(needed)
        }
        samples.writePcm16(bytes, count)
        pcm.write(bytes, 0, needed)
        framesWritten += frames
    }

    override fun close() {
        pcm.close()
        writeWav()
        SystemFileSystem.delete(temp, mustExist = false)
    }

    private fun writeWav() {
        val dataBytes = (framesWritten * channels * BYTES_PER_SAMPLE).toInt()
        SystemFileSystem.sink(target).buffered().use { sink ->
            sink.writeString("RIFF")
            sink.writeIntLe(RIFF_HEADER_BYTES + dataBytes)
            sink.writeString("WAVE")

            sink.writeString("fmt ")
            sink.writeIntLe(FMT_CHUNK_BYTES)
            sink.writeShortLe(PCM_INTEGER)
            sink.writeShortLe(channels.toShort())
            sink.writeIntLe(sampleRate)
            sink.writeIntLe(sampleRate * channels * BYTES_PER_SAMPLE)
            sink.writeShortLe((channels * BYTES_PER_SAMPLE).toShort())
            sink.writeShortLe(BITS_PER_SAMPLE)

            sink.writeString("data")
            sink.writeIntLe(dataBytes)

            SystemFileSystem.source(temp).buffered().use { source ->
                source.transferTo(sink)
            }
        }
    }

    private companion object {
        const val RIFF_HEADER_BYTES = 36
        const val FMT_CHUNK_BYTES = 16
        const val PCM_INTEGER: Short = 1
        const val BITS_PER_SAMPLE: Short = 16
    }
}
