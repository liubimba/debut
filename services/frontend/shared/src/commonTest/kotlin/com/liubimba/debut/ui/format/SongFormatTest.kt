package com.liubimba.debut.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SongFormatTest {
    @Test
    fun formatsSecondsAsMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0.0))
        assertEquals("1:02", formatDuration(62.4))
        assertEquals("3:42", formatDuration(221.5))
    }

    @Test
    fun formatsLongDurationsWithHours() {
        assertEquals("1:02:03", formatDuration(3723.0))
    }

    @Test
    fun formatsTakeDateWithoutTime() {
        assertEquals("2026-08-17", formatTakeDate(Instant.parse("2026-08-17T12:34:56Z")))
    }

    @Test
    fun namesMiddleCAsC4() {
        assertEquals("C4", formatNoteName(60))
    }

    @Test
    fun namesSharpsAndOctaves() {
        assertEquals("C#4", formatNoteName(61))
        assertEquals("E4", formatNoteName(64))
        assertEquals("A5", formatNoteName(81))
        assertEquals("B2", formatNoteName(47))
    }
}
