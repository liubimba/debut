package com.liubimba.debut.data.entity

import com.liubimba.debut.data.api.dto.NoteDTO
import kotlinx.serialization.Serializable

const val MINIMUM_PITCH_CONFIDENCE = 0.5f

@Serializable
data class VocalRange(val lowestMidi: Int, val highestMidi: Int)

fun List<NoteDTO>.vocalRange(
    minimumConfidence: Float = MINIMUM_PITCH_CONFIDENCE
): VocalRange? {
    val confident = filter { it.pitch.confidence >= minimumConfidence }.map { it.pitch.midi }
    if (confident.isEmpty()) {
        return null
    }
    return VocalRange(lowestMidi = confident.min(), highestMidi = confident.max())
}
