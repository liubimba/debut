package com.liubimba.debut.data.audio

import co.touchlab.kermit.Logger

class MixerTrack(
    val reader: WavReader,
    var gain: Float = 1f,
    var muted: Boolean = false
)

class StemMixer(private val tracks: List<MixerTrack>) : AutoCloseable {
    private val log = Logger.withTag("StemMixer")

    init {
        require(tracks.isNotEmpty()) { "mixer needs at least one track" }
    }

    val format: WavFormat = tracks.first().reader.format

    init {
        require(tracks.all { it.reader.format.sampleRate == format.sampleRate }) {
            "tracks disagree on sample rate"
        }
        require(tracks.all { it.reader.format.channels == format.channels }) {
            "tracks disagree on channels"
        }
    }

    private var scratch = FloatArray(0)

    fun seekTo(frame: Long) = tracks.forEach { it.reader.seekTo(frame) }

    fun mix(destination: FloatArray, frames: Int): Int {
        val samples = frames * format.channels
        if (scratch.size < samples) {
            scratch = FloatArray(samples)
        }
        destination.fill(0f, 0, samples)
        var produced = 0
        for (track in tracks) {
            val read = track.reader.readFrames(scratch, frames)
            produced = maxOf(produced, read)
            if (track.muted || track.gain == 0f) {
                continue
            }
            for (sample in 0 until read * format.channels) {
                destination[sample] += scratch[sample] * track.gain
            }
        }
        for (sample in 0 until produced * format.channels) {
            destination[sample] = destination[sample].coerceIn(-1f, 1f)
        }
        return produced
    }

    override fun close() = tracks.forEach { it.reader.close() }
}
