package com.liubimba.debut.data.entity

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class SongMetadata(
    val id: String,
    val name: String,
    val author: String? = null,
    val durationSeconds: Double? = null,
    val tempoBpm: Double? = null,
    val range: VocalRange? = null,
    val lastTakeAt: Instant? = null,
)
