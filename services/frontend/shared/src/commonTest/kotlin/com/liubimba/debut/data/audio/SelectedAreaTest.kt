package com.liubimba.debut.data.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelectedAreaTest {
    @Test
    fun aSelectionDraggedRightToLeftKeepsItsWidth() {
        val area = SelectedArea.of(start = 0.8f, end = 0.3f)

        assertEquals(0.3f, area.start)
        assertEquals(0.8f, area.end)
    }

    @Test
    fun fractionsOutsideTheTrackAreClamped() {
        val area = SelectedArea.of(start = -0.4f, end = 1.7f)

        assertEquals(0f, area.start)
        assertEquals(1f, area.end)
    }

    @Test
    fun theWholeTrackCountsAsNoSelection() {
        assertTrue(SelectedArea.Full.isFull)
        assertTrue(SelectedArea.of(0f, 1f).isFull)
        assertFalse(SelectedArea.of(0f, 0.5f).isFull)
    }
}
