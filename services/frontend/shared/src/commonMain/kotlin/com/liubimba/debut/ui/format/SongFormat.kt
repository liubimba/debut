package com.liubimba.debut.ui.format

import kotlin.math.roundToInt
import kotlin.time.Instant

private val NOTE_NAMES = listOf(
    "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
)

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3600
private const val SEMITONES_PER_OCTAVE = 12
private const val MIDI_OCTAVE_OFFSET = 1

fun formatDuration(seconds: Double): String {
    val total = seconds.roundToInt().coerceAtLeast(0)
    val hours = total / SECONDS_PER_HOUR
    val minutes = total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val remainder = total % SECONDS_PER_MINUTE
    return when {
        hours > 0 -> "$hours:${minutes.padded()}:${remainder.padded()}"
        else -> "$minutes:${remainder.padded()}"
    }
}

fun formatNoteName(midi: Int): String {
    val name = NOTE_NAMES[midi.mod(SEMITONES_PER_OCTAVE)]
    val octave = midi.floorDiv(SEMITONES_PER_OCTAVE) - MIDI_OCTAVE_OFFSET
    return "$name$octave"
}

fun formatTakeDate(instant: Instant): String = instant.toString().substringBefore('T')

private fun Int.padded(): String = toString().padStart(2, '0')
