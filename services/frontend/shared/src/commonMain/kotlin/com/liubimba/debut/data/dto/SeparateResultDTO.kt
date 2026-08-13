package com.liubimba.debut.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeparateResultDTO(
    @SerialName("stem_id") val stemId: String,
    val stems: List<String>,
)
