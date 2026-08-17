package com.liubimba.debut.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeparateResultDTO(
    @SerialName("stem_id") val stemId: String,
    val stems: List<String>,
    @SerialName("duration_seconds") val durationSeconds: Double? = null,
    @SerialName("tempo_bpm") val tempoBpm: Double? = null,
    val title: String? = null,
    val artist: String? = null,
)
