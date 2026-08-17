package com.liubimba.debut.data.storage

import co.touchlab.kermit.Logger
import com.liubimba.debut.data.api.dto.NoteDTO
import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.data.entity.StemType
import io.github.vinceglb.filekit.utils.div
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SongsStorage(private val root: Path) {
    private val json = Json { ignoreUnknownKeys = true }
    private val log = Logger.withTag(TAG)

    private val writes = Mutex()

    val meta = Meta()
    val stems = Stems()
    val notes = Notes()

    suspend fun delete(id: String): Unit = writes.withLock {
        meta.deleteUnlocked(id)
        stems.deleteUnlocked(id)
        notes.deleteUnlocked(id)
        log.i { "deleted song $id" }
    }

    inner class Meta {
        private val filepath = root.div("songs.meta.json")

        suspend fun readAll(): Map<String, SongMetadata> = withContext(ioDispatcher) {
            if (!SystemFileSystem.exists(filepath)) {
                log.i { "no metadata file at $filepath, starting empty" }
                return@withContext emptyMap()
            }
            runCatching {
                val text = SystemFileSystem.source(path = filepath).buffered().use {
                    it.readString()
                }
                json.decodeFromString<Map<String, SongMetadata>>(text)
            }.onSuccess { items ->
                log.d { "read ${items.size} songs from $filepath" }
            }.getOrElse { failure ->
                log.e(failure) { "unreadable metadata at $filepath, falling back to empty library" }
                emptyMap()
            }
        }

        suspend fun get(id: String): SongMetadata? = readAll()[id]

        suspend fun writeAll(items: Map<String, SongMetadata>): Unit = writes.withLock {
            persist(items)
        }

        suspend fun save(meta: SongMetadata): Unit = writes.withLock {
            persist(readAll() + (meta.id to meta))
        }

        suspend fun delete(id: String): Unit = writes.withLock {
            deleteUnlocked(id)
        }

        internal suspend fun deleteUnlocked(id: String) {
            persist(readAll() - id)
        }

        private suspend fun persist(items: Map<String, SongMetadata>) =
            withContext(ioDispatcher) {
                writeAtomically(filepath) {
                    it.writeString(json.encodeToString(items))
                }
                log.d { "persisted ${items.size} songs to $filepath" }
            }
    }

    inner class Stems {
        private val path = root.div("stems")

        suspend fun save(id: String, type: StemType, bytes: ByteArray): Path =
            writes.withLock {
                withContext(ioDispatcher) {
                    val target = path.div(id).div(type.name)
                    SystemFileSystem.createDirectories(path.div(id))
                    writeAtomically(target) {
                        it.write(bytes)
                    }
                    log.d { "saved $type stem of $id, ${bytes.size} bytes to $target" }
                    target
                }
            }

        suspend fun delete(id: String): Unit = writes.withLock {
            deleteUnlocked(id)
        }

        internal suspend fun deleteUnlocked(id: String) = withContext(ioDispatcher) {
            deleteRecursively(path.div(id))
            log.d { "deleted stems of $id" }
        }

        suspend fun get(id: String, type: StemType): ByteArray = withContext(ioDispatcher) {
            val source = path.div(id).div(type.name)
            val bytes = SystemFileSystem.source(source).buffered().readByteArray()
            log.d { "read $type stem of $id, ${bytes.size} bytes from $source" }
            bytes
        }

        fun has(id: String, type: StemType): Boolean =
            SystemFileSystem.exists(path.div(id).div((type.name)))
    }

    inner class Notes {
        private val path = root.div("notes")

        suspend fun save(id: String, notes: List<NoteDTO>): Path = writes.withLock {
            withContext(ioDispatcher) {
                val text = json.encodeToString(NotesFile(notes = notes))
                val target = writeAtomically(path.div(id).div("notes.json")) {
                    it.writeString(text)
                }
                log.d { "saved ${notes.size} notes of $id to $target" }
                target
            }
        }

        suspend fun delete(id: String): Unit = writes.withLock {
            deleteUnlocked(id)
        }

        internal suspend fun deleteUnlocked(id: String) = withContext(ioDispatcher) {
            deleteRecursively(path.div(id))
            log.d { "deleted notes of $id" }
        }

        suspend fun get(id: String): List<NoteDTO> = withContext(ioDispatcher) {
            val source = path.div(id).div("notes.json")
            val notes = SystemFileSystem.source(source).buffered().use {
                json.decodeFromString<List<NoteDTO>>(it.readString())
            }
            log.d { "read ${notes.size} notes of $id from $source" }
            notes
        }
    }

    private fun deleteRecursively(target: Path) {
        val metadata = SystemFileSystem.metadataOrNull(target) ?: return
        if (metadata.isDirectory) {
            SystemFileSystem.list(target).forEach { deleteRecursively(it) }
        }
        SystemFileSystem.delete(target, mustExist = false)
    }

    private inline fun writeAtomically(target: Path, write: (Sink) -> Unit): Path {
        SystemFileSystem.createDirectories(target.parent!!)
        val temp = Path("$target.${Random.nextInt(Int.MAX_VALUE)}.tmp")
        try {
            SystemFileSystem.sink(temp).buffered().use(write)
            SystemFileSystem.atomicMove(temp, target)
        } finally {
            SystemFileSystem.delete(temp, mustExist = false)
        }
        log.v { "wrote $target" }
        return target
    }

    private companion object {
        const val TAG = "SongsStorage"
    }
}

@Serializable
private data class NotesFile(val version: Int = 1, val notes: List<NoteDTO>)
