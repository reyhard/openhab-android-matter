package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class CommissioningWindowCountdownTest {
    @Test
    fun remainingSecondsCountsDownFromTimeout() {
        val countdown = CommissioningWindowCountdown(openedAtMillis = 1_000L, timeoutSeconds = 300)

        assertEquals(300, countdown.remainingSeconds(nowMillis = 1_000L))
        assertEquals(299, countdown.remainingSeconds(nowMillis = 1_001L))
        assertEquals(240, countdown.remainingSeconds(nowMillis = 61_000L))
    }

    @Test
    fun remainingSecondsNeverGoesNegative() {
        val countdown = CommissioningWindowCountdown(openedAtMillis = 1_000L, timeoutSeconds = 300)

        assertEquals(0, countdown.remainingSeconds(nowMillis = 302_000L))
    }

    @Test
    fun displayTextFormatsMinutesAndSeconds() {
        assertEquals("Pairing window open for 5:00", CommissioningWindowCountdown.displayText(300))
        assertEquals("Pairing window open for 0:09", CommissioningWindowCountdown.displayText(9))
    }
}
