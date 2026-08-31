package com.liubimba.debut.data.audio

import kotlin.concurrent.Volatile

class MixerTrack(
    val reader: FrameSource,
    var gain: Float = 1f,
    var muted: Boolean = false
)

class StemMixer(tracks: List<MixerTrack>) : AutoCloseable {
    init {
        require(tracks.isNotEmpty()) { "mixer needs at least one track" }
    }

    val format: WavFormat = tracks.first().reader.format

    @Volatile
    private var tracks: List<MixerTrack> = tracks

    init {
        tracks.forEach { agreeOnFormat(it) }
    }

    private var scratch = FloatArray(0)

    fun attach(track: MixerTrack) {
        agreeOnFormat(track)
        tracks = tracks + track
    }

    fun detach(track: MixerTrack) {
        tracks = tracks - track
    }

    fun seekTo(frame: Long) = tracks.forEach { it.reader.seekTo(frame) }

    fun mix(destination: FloatArray, frames: Int): Int {
        val mixed = tracks
        val samples = frames * format.channels
        if (scratch.size < samples) {
            scratch = FloatArray(samples)
        }
        destination.fill(0f, 0, samples)
        var produced = 0
        for (track in mixed) {
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

    private fun agreeOnFormat(track: MixerTrack) {
        require(track.reader.format.sampleRate == format.sampleRate) {
            "tracks disagree on sample rate"
        }
        require(track.reader.format.channels == format.channels) {
            "tracks disagree on channels"
        }
    }
}
