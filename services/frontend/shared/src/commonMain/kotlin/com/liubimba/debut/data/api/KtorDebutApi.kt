package com.liubimba.debut.data.api

import co.touchlab.kermit.Logger
import com.liubimba.debut.data.api.dto.JobDTO
import com.liubimba.debut.data.api.dto.StemDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.TimeSource
import io.ktor.client.plugins.logging.Logger as KtorLogger

class KtorDebutApi(
    val address: String = "127.0.0.1",
    val port: Int = 4999,
    val proto: String = "http",
) : IDebutApi, AutoCloseable {
    private val log = Logger.withTag(TAG)

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = object : KtorLogger {
                override fun log(message: String) = this@KtorDebutApi.log.d { message }
            }
            level = LogLevel.HEADERS
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 300_000
        }
    }

    override val audio: IDebutApi.IAudio = Audio()
    override val job: IDebutApi.IJob = Job()
    override val stem: IDebutApi.IStem = Stem()

    private fun root(): String {
        return this.proto + "://" + this.address + ":" + this.port + "/api/v1"
    }

    override fun close() {
        log.i { "closing http client" }
        client.close()
    }

    inner class Audio : IDebutApi.IAudio {
        override suspend fun separate(name: String, bytes: ByteArray): JobDTO =
            upload("${root()}/audio/separate", name, bytes)

        override suspend fun transcribe(name: String, bytes: ByteArray): JobDTO =
            upload("${root()}/audio/transcribe", name, bytes)

        private suspend fun upload(url: String, name: String, bytes: ByteArray): JobDTO {
            log.i { "uploading $name (${bytes.size} bytes) to $url" }
            val started = TimeSource.Monotonic.markNow()
            val job: JobDTO = client.submitFormWithBinaryData(
                url = url,
                formData = formData {
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                        append(HttpHeaders.ContentType, "audio/mpeg")
                    })
                },
            ).body()
            log.i { "uploaded $name in ${started.elapsedNow()}, job ${job.id} ${job.state}" }
            return job
        }
    }

    inner class Job : IDebutApi.IJob {
        override suspend fun get(jobId: String): JobDTO {
            val job: JobDTO = client.get("${root()}/jobs/$jobId").body()
            log.v { "job $jobId is ${job.state}" }
            return job
        }
    }

    inner class Stem : IDebutApi.IStem {
        override suspend fun find(stemId: String): List<StemDTO> {
            val stems: List<StemDTO> = client.get("${root()}/stems/$stemId").body()
            log.d { "stem $stemId has ${stems.size} files" }
            return stems
        }

        override suspend fun findAll(): Map<String, List<StemDTO>> {
            val stems: Map<String, List<StemDTO>> = client.get("${root()}/stems/").body()
            log.d { "backend holds ${stems.size} stems" }
            return stems
        }

        override suspend fun download(stemId: String, stemName: String): ByteArray {
            log.d { "downloading $stemId/$stemName" }
            val started = TimeSource.Monotonic.markNow()
            val bytes: ByteArray = client.get("${root()}/stems/$stemId/$stemName").body()
            log.i { "downloaded $stemId/$stemName, ${bytes.size} bytes in ${started.elapsedNow()}" }
            return bytes
        }
    }

    private companion object {
        const val TAG = "DebutApi"
    }
}
