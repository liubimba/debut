package com.liubimba.debut.data.audio

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WavReaderTest {
    private val root = Path(SystemTemporaryDirectory, "debut-wav-${Random.nextLong()}")

    @AfterTest
    fun cleanUp() = purge(root)

    @Test
    fun readsFormatOfAStereoFile() {
        val path = writeWav(root, "stereo.wav", ShortArray(400))

        WavReader(path).use { reader ->
            assertEquals(44100, reader.format.sampleRate)
            assertEquals(2, reader.format.channels)
            assertEquals(16, reader.format.bitsPerSample)
            assertEquals(200, reader.format.frameCount)
        }
    }

    @Test
    fun skipsUnknownChunksBetweenFormatAndData() {
        val samples = shortArrayOf(1000, -1000, 2000, -2000)
        val path = writeWav(root, "with-list.wav", samples, extraChunk = true)

        WavReader(path).use { reader ->
            assertEquals(2, reader.format.frameCount)
            val destination = FloatArray(4)
            assertEquals(2, reader.readFrames(destination, 2))
            assertTrue(abs(destination[0] - 1000f / 32768f) < 1e-6f)
            assertTrue(abs(destination[1] + 1000f / 32768f) < 1e-6f)
        }
    }

    @Test
    fun decodesTheFullSignedRange() {
        val path = writeWav(root, "range.wav", shortArrayOf(0, 32767, -32768, -1), channels = 1)

        WavReader(path).use { reader ->
            val destination = FloatArray(4)
            assertEquals(4, reader.readFrames(destination, 4))
            assertEquals(0f, destination[0])
            assertTrue(abs(destination[1] - 0.99997f) < 1e-4f)
            assertEquals(-1f, destination[2])
            assertTrue(abs(destination[3] + 1f / 32768f) < 1e-6f)
        }
    }

    @Test
    fun readFramesStopsAtTheEndOfData() {
        val path = writeWav(root, "short.wav", shortArrayOf(10, 20, 30, 40), channels = 1)

        WavReader(path).use { reader ->
            val destination = FloatArray(16)
            assertEquals(4, reader.readFrames(destination, 16))
            assertEquals(0, reader.readFrames(destination, 16))
        }
    }

    @Test
    fun readFramesNeverOverflowsTheDestination() {
        val path = writeWav(root, "long.wav", ShortArray(64) { it.toShort() }, channels = 2)

        WavReader(path).use { reader ->
            val destination = FloatArray(4)
            assertEquals(2, reader.readFrames(destination, 32))
        }
    }

    @Test
    fun seekToRewindsToTheSameSamples() {
        val path = writeWav(root, "seek.wav", shortArrayOf(100, 200, 300, 400), channels = 1)

        WavReader(path).use { reader ->
            val first = FloatArray(4)
            reader.readFrames(first, 4)

            reader.seekTo(2)
            val second = FloatArray(2)
            assertEquals(2, reader.readFrames(second, 2))
            assertEquals(first[2], second[0])
            assertEquals(first[3], second[1])
        }
    }
}
