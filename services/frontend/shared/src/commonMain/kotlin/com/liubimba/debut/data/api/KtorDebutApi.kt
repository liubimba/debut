package com.liubimba.debut.data.api

import com.liubimba.debut.data.dto.JobDTO
import com.liubimba.debut.data.dto.StemDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.append
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class KtorDebutApi(
    val address: String = "127.0.0.1",
    val port: Int = 4999,
    val proto: String = "http",
) : IDebutApi, AutoCloseable {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
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
        client.close()
    }

    inner class Audio : IDebutApi.IAudio {
        override suspend fun separate(name: String, bytes: ByteArray): JobDTO =
            upload("${root()}/audio/separate", name, bytes)

        override suspend fun transcribe(name: String, bytes: ByteArray): JobDTO =
            upload("${root()}/audio/transcribe", name, bytes)

        private suspend fun upload(url: String, name: String, bytes: ByteArray): JobDTO =
            client.submitFormWithBinaryData(
                url = url,
                formData = formData {
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                        append(HttpHeaders.ContentType, "audio/mpeg")
                    })
                },
            ).body()
    }

    inner class Job : IDebutApi.IJob {
        override suspend fun get(jobId: String): JobDTO =
            client.get("${root()}/jobs/$jobId").body()
    }

    inner class Stem : IDebutApi.IStem {
        override suspend fun find(stemId: String): List<StemDTO> =
            client.get("${root()}/stems/$stemId").body()

        override suspend fun findAll(): Map<String, List<StemDTO>> =
            client.get("${root()}/stems/").body()

        override suspend fun download(stemId: String, stemName: String): ByteArray =
            client.get("${root()}/stems/$stemId/$stemName").body()
    }
}
