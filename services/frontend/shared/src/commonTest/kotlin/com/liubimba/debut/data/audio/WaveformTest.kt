package com.liubimba.debut.data.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WaveformTest {
    @Test
    fun mergeTakesTheLoudestBucketOfEachStem() {
        val merged = Waveform.merge(
            listOf(
                Waveform(2048, floatArrayOf(0.1f, 0.9f, 0.2f)),
                Waveform(2048, floatArrayOf(0.5f, 0.4f, 0.1f)),
            )
        )

        assertEquals(listOf(0.5f, 0.9f, 0.2f), merged.peaks.toList())
        assertEquals(2048, merged.framesPerBucket)
    }

    @Test
    fun mergeKeepsTheLongestStem() {
        val merged = Waveform.merge(
            listOf(
                Waveform(2048, floatArrayOf(0.1f)),
                Waveform(2048, floatArrayOf(0.2f, 0.3f)),
            )
        )

        assertEquals(listOf(0.2f, 0.3f), merged.peaks.toList())
    }

    @Test
    fun mergeRejectsDifferentBucketSizes() {
        assertFailsWith<IllegalArgumentException> {
            Waveform.merge(
                listOf(
                    Waveform(2048, floatArrayOf(0.1f)),
                    Waveform(1024, floatArrayOf(0.2f)),
                )
            )
        }
    }

    @Test
    fun mergeRejectsAnEmptyList() {
        assertFailsWith<IllegalArgumentException> { Waveform.merge(emptyList()) }
    }

    @Test
    fun resampleTakesThePeakOfEachColumn() {
        val waveform = Waveform(2048, floatArrayOf(0.1f, 0.9f, 0.2f, 0.4f))

        assertEquals(listOf(0.9f, 0.4f), waveform.resampleTo(2).toList())
    }
}
