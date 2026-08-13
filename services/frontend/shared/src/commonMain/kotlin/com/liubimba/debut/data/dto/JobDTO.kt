package com.liubimba.debut.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
enum class JobStateDTO {
    QUEUED,
    RUNNING,
    FINISHED,
    FAILED,
}

@Serializable
data class JobDTO(
    val id: String,
    val state: JobStateDTO,
    @SerialName("error_message") val errorMessage: String? = null,
    val result: JsonElement? = null,
)

val jobJson: Json = Json { ignoreUnknownKeys = true }

inline fun <reified T> JobDTO.resultAs(): T? = result?.let { jobJson.decodeFromJsonElement(it) }
