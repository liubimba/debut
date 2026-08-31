package com.liubimba.debut.data.audio

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WavWriterTest {
    private val root = Path(SystemTemporaryDirectory, "debut-writer-${Random.nextLong()}")

    @AfterTest
    fun cleanUp() = purge(root)

    @Test
    fun writtenSamplesComeBackThroughTheReader() {
        val target = Path(root, "take.wav")

        WavWriter(target, sampleRate = 44100, channels = 1).use { writer ->
            writer.write(floatArrayOf(0f, 0.5f, -0.5f, 1f), frames = 4)
            assertEquals(4, writer.framesWritten)
        }

        WavReader(target).use { reader ->
            assertEquals(44100, reader.format.sampleRate)
            assertEquals(1, reader.format.channels)
            assertEquals(4, reader.format.frameCount)

            val destination = FloatArray(4)
            assertEquals(4, reader.readFrames(destination, 4))
            assertEquals(0f, destination[0])
            assertTrue(abs(destination[1] - 0.5f) < 1e-3f, "got ${destination[1]}")
            assertTrue(abs(destination[2] + 0.5f) < 1e-3f, "got ${destination[2]}")
        }
    }

    @Test
    fun theTargetDirectoryIsCreatedOnDemand() {
        val target = Path(Path(root, "takes"), "song", "take.wav")

        WavWriter(target, sampleRate = 22050, channels = 1).use { writer ->
            writer.write(floatArrayOf(0.25f, 0.25f), frames = 2)
        }

        assertTrue(SystemFileSystem.exists(target))
    }

    @Test
    fun theTemporaryFileIsRemovedAfterClosing() {
        val target = Path(root, "clean.wav")

        WavWriter(target, sampleRate = 44100, channels = 2).use { writer ->
            writer.write(FloatArray(8), frames = 4)
        }

        assertFalse(SystemFileSystem.exists(Path("$target.pcm")))
    }
}
