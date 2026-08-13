package com.liubimba.debut.data.repository

import com.liubimba.debut.data.api.IDebutApi
import com.liubimba.debut.data.dto.JobDTO
import com.liubimba.debut.data.dto.JobStateDTO
import com.liubimba.debut.data.dto.NoteDTO
import com.liubimba.debut.data.dto.SeparateResultDTO
import com.liubimba.debut.data.dto.resultAs
import kotlinx.coroutines.delay

enum class ImportStage {
    UPLOADING,
    SEPARATING,
    DOWNLOADING_VOCALS,
    TRANSCRIBING,
}

data class ImportedSong(
    val stemId: String,
    val title: String,
    val notes: List<NoteDTO>,
)


class SongsRepository(
    private val api: IDebutApi,
    private val pollIntervalMillis: Long = 1_000
) {
    suspend fun import(
        name: String,
        bytes: ByteArray,
        onStage: (ImportStage) -> Unit = {}
    ): ImportedSong {
        onStage(ImportStage.UPLOADING)
        val separateJob = api.audio.separate(name = name, bytes = bytes)

        onStage(ImportStage.SEPARATING)
        val separated = awaitJob(jobId = separateJob.id).resultAs<SeparateResultDTO>()
            ?: throw IllegalStateException("separation returned no result")

        onStage(ImportStage.DOWNLOADING_VOCALS)
        val vocals = api.stem.download(separated.stemId, VOCALS_STEM)

        onStage(ImportStage.TRANSCRIBING)
        val transcribeJob = api.audio.transcribe(VOCALS_STEM, vocals)
        val notes = awaitJob(transcribeJob.id).resultAs<List<NoteDTO>>().orEmpty()

        return ImportedSong(stemId = separated.stemId, title = name, notes = notes)
    }

    private suspend fun awaitJob(jobId: String): JobDTO {
        while (true) {
            val job = api.job.get(jobId = jobId)
            when (job.state) {
                JobStateDTO.FINISHED -> return job
                JobStateDTO.FAILED -> throw IllegalStateException(job.errorMessage ?: "job failed")
                else -> delay(pollIntervalMillis)
            }
        }
    }

    private companion object {
        const val VOCALS_STEM = "vocals.wav"
    }
}
