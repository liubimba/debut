package com.liubimba.debut.data.audio

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
class Waveform(
    val framesPerBucket: Int,
    val peaks: FloatArray
) {
    fun resampleTo(columns: Int): FloatArray {
        if (columns <= 0 || peaks.isEmpty()) {
            return FloatArray(0)
        }
        val result = FloatArray(columns)
        for (column in 0 until columns) {
            val from = (column.toLong() * peaks.size / columns).toInt()
            val to = ((column + 1).toLong() * peaks.size / columns).toInt().coerceAtLeast(from + 1)
            var peak = VOID
            for (index in from until minOf(to, peaks.size)) {
                if (peaks[index] > peak) {
                    peak = peaks[index]
                }
            }
            result[column] = peak
        }
        return result
    }

    fun placedAt(startFrame: Long, songFrames: Long): Waveform {
        val total = bucketsFor(songFrames)
        val before = (startFrame / framesPerBucket).toInt()
        require(before <= total) {
            "take at bucket $before does not fit in $total buckets"
        }
        val after = (total - before - peaks.size).coerceAtLeast(0)
        return void(before, framesPerBucket) + this + void(after, framesPerBucket)
    }

    private fun bucketsFor(frames: Long): Int =
        ((frames + framesPerBucket - 1) / framesPerBucket).toInt()

    operator fun plus(oth: Waveform): Waveform {
        require(oth.framesPerBucket == framesPerBucket) {
            "waveforms disagree on bucket size"
        }
        return Waveform(framesPerBucket, peaks + oth.peaks)
    }

    companion object {
        const val DEFAULT_FRAMES_PER_BUCKET = 2048

        const val VOID = -1f
        fun void(
            size: Int = 0,
            framesPerBucket: Int = DEFAULT_FRAMES_PER_BUCKET
        ): Waveform {
            return Waveform(framesPerBucket, FloatArray(size, { _ -> VOID }))
        }

        fun merge(waveforms: List<Waveform>): Waveform {
            require(waveforms.isNotEmpty()) { "nothing to merge" }
            val framesPerBucket = waveforms.first().framesPerBucket
            require(waveforms.all { it.framesPerBucket == framesPerBucket }) {
                "waveforms disagree on bucket size"
            }
            val merged = FloatArray(waveforms.maxOf { it.peaks.size })
            for (waveform in waveforms) {
                waveform.peaks.forEachIndexed { index, peak ->
                    if (peak > merged[index]) {
                        merged[index] = peak
                    }
                }
            }
            return Waveform(framesPerBucket, merged)
        }

        fun of(
            reader: WavReader,
            framesPerBucket: Int = DEFAULT_FRAMES_PER_BUCKET
        ): Waveform {
            val channels = reader.format.channels
            val chunk = FloatArray(framesPerBucket * channels)
            val buckets = ArrayList<Float>(
                (reader.format.frameCount / framesPerBucket + 1).toInt()
            )
            reader.seekTo(0)
            while (true) {
                val produced = reader.readFrames(chunk, framesPerBucket)
                if (produced == 0) {
                    break
                }
                var peak = 0f
                for (sample in 0 until produced * channels) {
                    val value = abs(chunk[sample])
                    if (value > peak) {
                        peak = value
                    }
                }
                buckets.add(peak)
            }
            return Waveform(framesPerBucket, buckets.toFloatArray())
        }
    }
}
