package com.learning.mylittleschool

import org.junit.Assert.assertEquals
import org.junit.Test

class WorksheetTimerTest {
    @Test
    fun timedWorksheetGetsOneMinutePerQuestionWithThreeToTenMinuteBounds() {
        assertEquals(180, WorksheetTimer.durationSeconds(0))
        assertEquals(180, WorksheetTimer.durationSeconds(1))
        assertEquals(300, WorksheetTimer.durationSeconds(5))
        assertEquals(600, WorksheetTimer.durationSeconds(20))
    }

    @Test
    fun remainingTimeUsesMinutesAndSeconds() {
        assertEquals("05:00", WorksheetTimer.formatRemaining(300))
        assertEquals("01:07", WorksheetTimer.formatRemaining(67))
        assertEquals("00:00", WorksheetTimer.formatRemaining(-1))
    }
}
