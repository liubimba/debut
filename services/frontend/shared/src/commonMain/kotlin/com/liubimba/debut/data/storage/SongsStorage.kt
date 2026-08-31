package com.liubimba.debut.data.storage

import co.touchlab.kermit.Logger
import com.liubimba.debut.data.api.dto.NoteDTO
import com.liubimba.debut.data.audio.Waveform
import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.data.entity.StemType
import com.liubimba.debut.data.entity.TakeMetadata
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

class SongsStorage(private val root: Path) {
    private val json = Json { ignoreUnknownKeys = true }
    private val log = Logger.withTag(TAG)

    private val writes = Mutex()

    val meta = Meta()
    val stems = Stems()
    val notes = Notes()
    val takes = Takes()
    val waveforms = Waveforms()

    suspend fun delete(id: String): Unit = writes.withLock {
        meta.deleteUnlocked(id)
        stems.deleteUnlocked(id)
        notes.deleteUnlocked(id)
        takes.deleteUnlocked(id)
        waveforms.deleteUnlocked(id)
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

        fun pathOf(id: String, type: StemType): Path = path.div(id).div(type.name)

        suspend fun get(id: String, type: StemType): ByteArray = withContext(ioDispatcher) {
            val source = path.div(id).div(type.name)
            val bytes = SystemFileSystem.source(source).buffered().readByteArray()
            log.d { "read $type stem of $id, ${bytes.size} bytes from $source" }
            bytes
        }

        fun has(id: String, type: StemType): Boolean =
            SystemFileSystem.exists(pathOf(id, type))
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

    inner class Takes {
        private val path = root.div("takes")
        fun pathOf(songId: String, takeId: String): Path =
            path.div(songId).div("$takeId.wav")

        suspend fun readAll(songId: String): Map<String, TakeMetadata> =
            withContext(ioDispatcher) {
                val source = indexOf(songId)
                if (!SystemFileSystem.exists(source)) {
                    return@withContext emptyMap()
                }
                runCatching {
                    SystemFileSystem.source(source).buffered().use {
                        json.decodeFromString<Map<String, TakeMetadata>>(it.readString())
                    }
                }.getOrElse { failure ->
                    log.e(failure) { "unreadable takes index at $source" }
                    emptyMap()
                }
            }

        suspend fun save(songId: String, take: TakeMetadata): Unit = writes.withLock {
            val items = readAll(songId) + (take.id to take)
            withContext(ioDispatcher) {
                writeAtomically(indexOf(songId)) {
                    it.writeString(json.encodeToString(items))
                }
                log.d { "saved take ${take.id} of $songId, ${take.frameCount} frames" }
            }
        }

        suspend fun delete(songId: String): Unit = writes.withLock {
            deleteUnlocked(songId)
        }

        internal suspend fun deleteUnlocked(songId: String) = withContext(ioDispatcher) {
            deleteRecursively(path.div(songId))
            log.d { "deleted takes of $songId" }
        }

        private fun indexOf(songId: String): Path = path.div(songId).div("takes.json")
    }

    inner class Waveforms {
        private val path = root.div("waveforms")

        suspend fun save(id: String, type: StemType, waveform: Waveform): Path =
            writes.withLock {
                withContext(ioDispatcher) {
                    val target = writeAtomically(path.div(id).div(type.name)) {
                        it.writeString(json.encodeToString(waveform))
                    }
                    log.d { "saved ${waveform.peaks.size} buckets of $type/$id to $target" }
                    target
                }
            }

        suspend fun get(id: String, type: StemType): Waveform? = withContext(ioDispatcher) {
            val source = path.div(id).div(type.name)
            if (!SystemFileSystem.exists(source)) {
                return@withContext null
            }
            runCatching {
                SystemFileSystem.source(source).buffered().use {
                    json.decodeFromString<Waveform>(it.readString())
                }
            }.onSuccess { waveform ->
                log.d { "read ${waveform.peaks.size} buckets of $type/$id from $source" }
            }.getOrElse { failure ->
                log.e(failure) { "unreadable waveform at $source, it will be recomputed" }
                null
            }
        }

        suspend fun delete(id: String): Unit = writes.withLock {
            deleteUnlocked(id)
        }

        internal suspend fun deleteUnlocked(id: String) = withContext(ioDispatcher) {
            deleteRecursively(path.div(id))
            log.d { "deleted waveforms of $id" }
        }
    }


    private companion object {
        const val TAG = "SongsStorage"
    }
}

@Serializable
private data class NotesFile(val version: Int = 1, val notes: List<NoteDTO>)
