package org.openhab.matter.companion.setup

data class CommissioningWindowCountdown(
    val openedAtMillis: Long,
    val timeoutSeconds: Int
) {
    fun remainingSeconds(nowMillis: Long): Int {
        val elapsedMillis = (nowMillis - openedAtMillis).coerceAtLeast(0L)
        val elapsedSeconds = if (elapsedMillis == 0L) 0L else ((elapsedMillis - 1L) / 1_000L) + 1L
        return (timeoutSeconds - elapsedSeconds).coerceAtLeast(0L).toInt()
    }

    companion object {
        fun displayText(remainingSeconds: Int): String {
            val safeSeconds = remainingSeconds.coerceAtLeast(0)
            val minutes = safeSeconds / 60
            val seconds = safeSeconds % 60
            return "Pairing window open for $minutes:${seconds.toString().padStart(2, '0')}"
        }
    }
}
