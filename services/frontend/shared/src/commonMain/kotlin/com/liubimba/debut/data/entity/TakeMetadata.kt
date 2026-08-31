package com.liubimba.debut.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class TakeMetadata(
    val id: String,
    val startFrame: Long,
    val frameCount: Long,
)
