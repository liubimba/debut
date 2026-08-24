package com.liubimba.debut.data.audio

import kotlin.test.Test
import kotlin.test.assertEquals

class SongPlayerTest {
    @Test
    fun beforeAnySeekThePositionIsWhatWasPlayed() {
        assertEquals(
            4410,
            songFrame(seekOrigin = 0, framesPlayedAtSeek = 0, framesPlayed = 4410)
        )
    }

    @Test
    fun afterASeekThePositionCountsFromTheSeekTarget() {
        assertEquals(
            44100 + 2205,
            songFrame(seekOrigin = 44100, framesPlayedAtSeek = 88200, framesPlayed = 90405)
        )
    }

    @Test
    fun aSeekWithNothingPlayedYetLandsExactlyOnTheTarget() {
        assertEquals(
            44100,
            songFrame(seekOrigin = 44100, framesPlayedAtSeek = 88200, framesPlayed = 88200)
        )
    }
}
