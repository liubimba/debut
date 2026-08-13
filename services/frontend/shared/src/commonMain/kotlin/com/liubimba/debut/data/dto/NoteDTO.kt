package com.liubimba.debut.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteDTO(
    @SerialName(value = "start_time") val startTime: Float,
    @SerialName(value = "end_time") val endTime: Float,
    val pitch: PitchDTO
)
