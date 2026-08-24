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
            var peak = 0f
            for (index in from until minOf(to, peaks.size)) {
                if (peaks[index] > peak) {
                    peak = peaks[index]
                }
            }
            result[column] = peak
        }
        return result
    }

    companion object {
        const val DEFAULT_FRAMES_PER_BUCKET = 2048

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
