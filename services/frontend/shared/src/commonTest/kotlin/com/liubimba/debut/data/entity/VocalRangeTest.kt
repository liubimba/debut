package com.liubimba.debut.data.entity

import com.liubimba.debut.data.api.dto.NoteDTO
import com.liubimba.debut.data.api.dto.PitchDTO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun note(midi: Int, confidence: Float = 0.9f): NoteDTO = NoteDTO(
    startTime = 0f,
    endTime = 1f,
    pitch = PitchDTO(
        midi = midi,
        freqHz = 440f,
        centsOffset = 0f,
        confidence = confidence,
    ),
)

class VocalRangeTest {
    @Test
    fun spansTheLowestAndHighestNote() {
        val range = listOf(note(64), note(60), note(67)).vocalRange()

        assertEquals(VocalRange(lowestMidi = 60, highestMidi = 67), range)
    }

    @Test
    fun ignoresNotesBelowTheConfidenceThreshold() {
        val notes = listOf(note(60), note(64), note(96, confidence = 0.1f))

        assertEquals(VocalRange(lowestMidi = 60, highestMidi = 64), notes.vocalRange())
    }

    @Test
    fun hasNoRangeWithoutConfidentNotes() {
        assertNull(listOf(note(60, confidence = 0.1f)).vocalRange())
    }

    @Test
    fun hasNoRangeWithoutNotes() {
        assertNull(emptyList<NoteDTO>().vocalRange())
    }
}
