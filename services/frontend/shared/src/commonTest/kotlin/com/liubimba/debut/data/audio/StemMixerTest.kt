package com.liubimba.debut.data.audio

import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StemMixerTest {
    private val root = Path(SystemTemporaryDirectory, "debut-mixer-${Random.nextLong()}")

    @AfterTest
    fun cleanUp() = purge(root)

    private fun track(name: String, samples: ShortArray, sampleRate: Int = 44100): MixerTrack =
        MixerTrack(WavReader(writeWav(root, name, samples, sampleRate, channels = 1)))

    @Test
    fun oppositeSignalsCancelOut() {
        val mixer = StemMixer(
            listOf(
                track("plus.wav", ShortArray(4) { 10000 }),
                track("minus.wav", ShortArray(4) { -10000 }),
            )
        )

        val destination = FloatArray(4)
        mixer.use {
            assertEquals(4, it.mix(destination, 4))
        }

        destination.forEach { assertTrue(abs(it) < 1e-6f, "expected silence, got $it") }
    }

    @Test
    fun gainScalesTheContribution() {
        val quiet = track("quiet.wav", ShortArray(4) { 10000 }).apply { gain = 0.5f }
        val destination = FloatArray(4)

        StemMixer(listOf(quiet)).use { it.mix(destination, 4) }

        assertTrue(abs(destination[0] - 5000f / 32768f) < 1e-4f, "got ${destination[0]}")
    }

    @Test
    fun mutedTrackIsSilentButKeepsItsPosition() {
        val muted = track("muted.wav", shortArrayOf(1000, 2000, 3000, 4000))
        muted.muted = true
        val destination = FloatArray(2)

        StemMixer(listOf(muted)).use { mixer ->
            mixer.mix(destination, 2)
            assertEquals(0f, destination[0])

            muted.muted = false
            mixer.mix(destination, 2)
            assertTrue(abs(destination[0] - 3000f / 32768f) < 1e-4f, "got ${destination[0]}")
        }
    }

    @Test
    fun theSumIsClampedToTheValidRange() {
        val mixer = StemMixer(
            listOf(
                track("loud-one.wav", ShortArray(4) { 30000 }),
                track("loud-two.wav", ShortArray(4) { 30000 }),
            )
        )

        val destination = FloatArray(4)
        mixer.use { it.mix(destination, 4) }

        destination.forEach { assertEquals(1f, it) }
    }

    @Test
    fun anEmptyTrackListIsRejected() {
        assertFailsWith<IllegalArgumentException> { StemMixer(emptyList()) }
    }

    @Test
    fun tracksWithDifferentSampleRatesAreRejected() {
        val tracks = listOf(
            track("fast.wav", ShortArray(4) { 100 }, sampleRate = 44100),
            track("slow.wav", ShortArray(4) { 100 }, sampleRate = 22050),
        )

        assertFailsWith<IllegalArgumentException> { StemMixer(tracks) }
    }
}
