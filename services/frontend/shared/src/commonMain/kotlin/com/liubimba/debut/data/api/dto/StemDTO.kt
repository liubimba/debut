package com.liubimba.debut.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class StemDTO(
    val name: String,
    val url: String
)
