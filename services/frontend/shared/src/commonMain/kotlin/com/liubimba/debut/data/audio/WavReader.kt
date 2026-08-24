package com.liubimba.debut.data.audio

import co.touchlab.kermit.Logger
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.readIntLe
import kotlinx.io.readShortLe
import kotlinx.io.readString

data class WavFormat(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val frameCount: Long
) {
    val durationSeconds: Double get() = frameCount.toDouble() / sampleRate
}


class WavReader(private val path: Path) : AutoCloseable {
    private val log = Logger.withTag("WavReader")
    private val layout = SystemFileSystem.source(path).buffered().use { readLayout(it) }

    val format: WavFormat = layout.format

    private var source: Source = openAt(0)
    private var position: Long = 0

    fun seekTo(frame: Long) {
        source.close()
        position = frame.coerceIn(0, format.frameCount)
        source = openAt(position)
    }

    fun readFrames(destination: FloatArray, frames: Int): Int {
        val capacity = (destination.size / format.channels).toLong()
        val available =
            minOf(frames.toLong(), capacity, format.frameCount - position).toInt()
        log.d { "capacity $capacity available $available" }
        if (available <= 0) {
            return 0
        }
        val bytes = source.readByteArray(available * format.channels * BYTES_PER_SAMPLE)
        var cursor = 0
        for (sample in 0 until available * format.channels) {
            val low = bytes[cursor++].toInt() and 0xFF
            val high = bytes[cursor++].toInt()
            destination[sample] = ((high shl 8) or low) / SAMPLE_SCALE
        }
        position += available
        log.d { "position $position" }
        return available
    }

    override fun close() = source.close()

    private class Layout(val format: WavFormat, val dataOffset: Long)

    private fun openAt(frame: Long): Source =
        SystemFileSystem.source(path).buffered().apply {
            skip(layout.dataOffset + frame * format.channels * BYTES_PER_SAMPLE)
        }

    private fun readLayout(source: Source): Layout {
        require(source.readString(4) == "RIFF") { "not a RIFF file: $path" }
        source.skip(4)
        require(source.readString(4) == "WAVE") { "not a WAVE file: $path" }

        var channels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var offset = 12L

        while (true) {
            val id = source.readString(4)
            val size = source.readIntLe().toLong() and 0xFFFFFFFFL
            offset += 8
            when (id) {
                "fmt " -> {
                    val encoding = source.readShortLe().toInt()
                    channels = source.readShortLe().toInt()
                    sampleRate = source.readIntLe()
                    source.skip(6)
                    bitsPerSample = source.readShortLe().toInt()
                    require(encoding == PCM_INTEGER) {
                        "unsupported encoding $encoding in $path"
                    }
                    require(bitsPerSample == 16) {
                        "unsupported bit depth $bitsPerSample in $path"
                    }
                    source.skip(size - FMT_HEADER_BYTES)
                    offset += size
                }

                "data" -> {
                    require(channels > 0) { "data chunk before fmt chunk in $path" }
                    return Layout(
                        format = WavFormat(
                            sampleRate = sampleRate,
                            channels = channels,
                            bitsPerSample = bitsPerSample,
                            frameCount = size / (channels * BYTES_PER_SAMPLE)
                        ),
                        dataOffset = offset
                    )
                }

                else -> {
                    val padded = size + (size and 1)
                    source.skip(padded)
                    offset += padded
                }
            }
        }
    }

    private companion object {
        const val BYTES_PER_SAMPLE = 2
        const val SAMPLE_SCALE = 32768f
        const val PCM_INTEGER = 1
        const val FMT_HEADER_BYTES = 16L
    }
}
