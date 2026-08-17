package com.liubimba.debut.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PitchDTO(
    val midi: Int,
    @SerialName(value = "freq_hz") val freqHz: Float,
    @SerialName(value = "cents_offset") val centsOffset: Float,
    @SerialName(value = "confidence") val confidence: Float
) {
}
