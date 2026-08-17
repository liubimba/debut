package com.liubimba.debut.data.api

import com.liubimba.debut.data.api.dto.JobDTO
import com.liubimba.debut.data.api.dto.StemDTO

interface IDebutApi {
    val audio: IAudio
    val job: IJob
    val stem: IStem

    interface IAudio {
        suspend fun separate(name: String, bytes: ByteArray): JobDTO

        suspend fun transcribe(name: String, bytes: ByteArray): JobDTO
    }

    interface IJob {
        suspend fun get(jobId: String): JobDTO
    }

    interface IStem {
        suspend fun find(stemId: String): List<StemDTO>

        suspend fun findAll(): Map<String, List<StemDTO>>

        suspend fun download(stemId: String, stemName: String): ByteArray
    }
}
