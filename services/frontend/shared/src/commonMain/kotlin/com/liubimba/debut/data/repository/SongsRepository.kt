package com.liubimba.debut.data.repository

import co.touchlab.kermit.Logger
import com.liubimba.debut.data.api.IDebutApi
import com.liubimba.debut.data.api.dto.JobDTO
import com.liubimba.debut.data.api.dto.JobStateDTO
import com.liubimba.debut.data.api.dto.NoteDTO
import com.liubimba.debut.data.api.dto.SeparateResultDTO
import com.liubimba.debut.data.api.dto.resultAs
import com.liubimba.debut.data.entity.SongMetadata
import com.liubimba.debut.data.entity.StemType
import com.liubimba.debut.data.entity.vocalRange
import com.liubimba.debut.data.storage.SongsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

enum class ImportStage {
    UPLOADING,
    SEPARATING,
    DOWNLOADING_VOCALS,
    DOWNLOADING_BASS,
    DOWNLOADING_DRUMS,
    DOWNLOADING_OTHER,
    TRANSCRIBING,
}

class SongsRepository(
    private val api: IDebutApi,
    private val pollIntervalMillis: Long = 1_000,
    private val localStorage: SongsStorage,
    scope: CoroutineScope
) {
    private val log = Logger.withTag(TAG)

    private var _songs = MutableStateFlow<Map<String, SongMetadata>>(emptyMap())

    val songs = _songs.asStateFlow()

    init {
        scope.launch { load() }
    }

    suspend fun import(
        name: String,
        bytes: ByteArray,
        onStage: (ImportStage) -> Unit = {}
    ): SongMetadata {
        val started = TimeSource.Monotonic.markNow()
        log.i { "importing $name (${bytes.size} bytes)" }

        onStage(ImportStage.UPLOADING)
        val separateJob = api.audio.separate(name = name, bytes = bytes)

        onStage(ImportStage.SEPARATING)
        val separated = awaitJob(jobId = separateJob.id).resultAs<SeparateResultDTO>()
            ?: throw IllegalStateException("separation returned no result")
        log.i { "separated $name into stem ${separated.stemId}: ${separated.stems}" }

        onStage(ImportStage.DOWNLOADING_VOCALS)
        val vocals = api.stem.download(separated.stemId, VOCALS_STEM)
        val vocalsPath =
            localStorage.stems.save(id = separated.stemId, type = StemType.VOCALS, bytes = vocals)
        log.d { "stored vocals at $vocalsPath" }

        onStage(ImportStage.DOWNLOADING_BASS)
        val bass = api.stem.download(separated.stemId, BASS_STEM)
        val bassPath =
            localStorage.stems.save(id = separated.stemId, type = StemType.BASS, bytes = bass)
        log.d { "stored bass at $bassPath" }

        onStage(ImportStage.DOWNLOADING_DRUMS)
        val drums = api.stem.download(separated.stemId, DRUMS_STEM)
        val drumsPath =
            localStorage.stems.save(id = separated.stemId, type = StemType.DRUMS, bytes = drums)
        log.d { "stored drums at $drumsPath" }

        onStage(ImportStage.DOWNLOADING_OTHER)
        val other = api.stem.download(separated.stemId, OTHER_STEM)
        val otherPath =
            localStorage.stems.save(id = separated.stemId, type = StemType.OTHER, bytes = other)
        log.d { "stored other at $otherPath" }

        onStage(ImportStage.TRANSCRIBING)
        val transcribeJob = api.audio.transcribe(VOCALS_STEM, vocals)
        val notes = awaitJob(transcribeJob.id).resultAs<List<NoteDTO>>().orEmpty()
        if (notes.isEmpty()) {
            log.w { "transcription of ${separated.stemId} returned no notes" }
        }
        localStorage.notes.save(id = separated.stemId, notes = notes)

        val songMetadata = SongMetadata(
            id = separated.stemId,
            name = separated.title ?: name.substringBeforeLast('.'),
            author = separated.artist,
            durationSeconds = separated.durationSeconds,
            tempoBpm = separated.tempoBpm,
            range = notes.vocalRange(),
        )
        localStorage.meta.save(meta = songMetadata)

        _songs.update { current -> current + (songMetadata.id to songMetadata) }
        log.i {
            "imported $name as ${songMetadata.id} with ${notes.size} notes in ${started.elapsedNow()}"
        }

        return songMetadata
    }

    suspend fun delete(id: String) {
        localStorage.delete(id)
        _songs.update { current -> current - id }
        log.i { "deleted song $id" }
    }

    suspend fun load() {
        val started = TimeSource.Monotonic.markNow()
        val stored = localStorage.meta.readAll()
        _songs.value = stored
        log.i { "loaded ${stored.size} songs in ${started.elapsedNow()}" }
    }

    private suspend fun awaitJob(jobId: String): JobDTO {
        val started = TimeSource.Monotonic.markNow()
        var reported: JobStateDTO? = null
        while (true) {
            val job = api.job.get(jobId = jobId)
            if (job.state != reported) {
                log.d { "job $jobId entered ${job.state} after ${started.elapsedNow()}" }
                reported = job.state
            }
            when (job.state) {
                JobStateDTO.FINISHED -> {
                    log.i { "job $jobId finished in ${started.elapsedNow()}" }
                    return job
                }

                JobStateDTO.FAILED -> {
                    log.e { "job $jobId failed after ${started.elapsedNow()}: ${job.errorMessage}" }
                    throw IllegalStateException(job.errorMessage ?: "job failed")
                }

                else -> delay(pollIntervalMillis)
            }
        }
    }

    private companion object {
        const val TAG = "SongsRepository"
        const val VOCALS_STEM = "vocals.wav"
        const val DRUMS_STEM = "drums.wav"
        const val OTHER_STEM = "other.wav"
        const val BASS_STEM = "bass.wav"
    }

}
