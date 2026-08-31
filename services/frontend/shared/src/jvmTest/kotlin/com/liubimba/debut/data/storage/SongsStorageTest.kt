package com.liubimba.debut.data.storage

import com.liubimba.debut.data.audio.Waveform
import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.data.entity.StemType
import com.liubimba.debut.data.entity.TakeMetadata
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SongsStorageTest {
    private val root = Path(SystemTemporaryDirectory, "debut-${Random.nextLong()}")
    private val storage = SongsStorage(root)

    @AfterTest
    fun cleanUp() {
        purge(root)
    }

    private fun purge(target: Path) {
        val metadata = SystemFileSystem.metadataOrNull(target) ?: return
        if (metadata.isDirectory) {
            SystemFileSystem.list(target).forEach { purge(it) }
        }
        SystemFileSystem.delete(target, mustExist = false)
    }

    @Test
    fun savedMetadataSurvivesReading() = runBlocking {
        storage.meta.save(SongMetadata(id = "song", name = "Runaway"))

        assertEquals("Runaway", storage.meta.readAll()["song"]?.name)
    }

    @Test
    fun deleteRemovesMetadataStemsAndNotes() = runBlocking {
        storage.meta.save(SongMetadata(id = "song", name = "Runaway"))
        storage.stems.save("song", StemType.VOCALS, byteArrayOf(1, 2, 3))
        storage.stems.save("song", StemType.DRUMS, byteArrayOf(4, 5, 6))
        storage.notes.save("song", emptyList())
        assertTrue(storage.stems.has("song", StemType.VOCALS))

        storage.delete("song")

        assertFalse(storage.stems.has("song", StemType.VOCALS))
        assertFalse(storage.stems.has("song", StemType.DRUMS))
        assertTrue(storage.meta.readAll().isEmpty())
    }

    @Test
    fun savedWaveformIsReadBack() = runBlocking {
        val waveform = Waveform(framesPerBucket = 2048, peaks = floatArrayOf(0.1f, 0.9f, 0.4f))

        storage.waveforms.save("song", StemType.VOCALS, waveform)
        val restored = storage.waveforms.get("song", StemType.VOCALS)

        assertEquals(2048, restored?.framesPerBucket)
        assertEquals(listOf(0.1f, 0.9f, 0.4f), restored?.peaks?.toList())
    }

    @Test
    fun missingWaveformIsNull() = runBlocking {
        assertNull(storage.waveforms.get("song", StemType.DRUMS))
    }

    @Test
    fun deleteRemovesWaveformsToo() = runBlocking {
        storage.waveforms.save("song", StemType.VOCALS, Waveform(2048, floatArrayOf(0.5f)))

        storage.delete("song")

        assertNull(storage.waveforms.get("song", StemType.VOCALS))
    }

    @Test
    fun savedTakeIsReadBackAndDeletedWithTheSong() = runBlocking {
        val take = TakeMetadata(id = "take-1", startFrame = 44100, frameCount = 22050)

        storage.takes.save("song", take)

        assertEquals(take, storage.takes.readAll("song")["take-1"])
        assertTrue(storage.takes.pathOf("song", "take-1").toString().endsWith("take-1.wav"))

        storage.delete("song")

        assertTrue(storage.takes.readAll("song").isEmpty())
    }

    @Test
    fun deleteOfAnUnknownSongIsNotAnError() = runBlocking {
        storage.delete("never-imported")

        assertTrue(storage.meta.readAll().isEmpty())
    }
}
