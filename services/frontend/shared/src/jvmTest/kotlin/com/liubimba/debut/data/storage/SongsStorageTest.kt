package com.liubimba.debut.data.storage

import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.data.entity.StemType
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun deleteOfAnUnknownSongIsNotAnError() = runBlocking {
        storage.delete("never-imported")

        assertTrue(storage.meta.readAll().isEmpty())
    }
}
