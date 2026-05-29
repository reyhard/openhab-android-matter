package org.openhab.matter.companion.setup

import org.junit.Assert.assertTrue
import org.junit.Test

class MatterSetupFailureTest {
    @Test
    fun inboxTimeoutSuggestionsMentionOtbrMdnsStaleRecordsAndOcwRetry() {
        val suggestions = MatterSetupFailure.defaultSuggestions(MatterSetupStage.WatchingOpenHabInbox)
            .joinToString(" ")

        assertTrue(suggestions.contains("IPv6"))
        assertTrue(suggestions.contains("OTBR"))
        assertTrue(suggestions.contains("mDNS"))
        assertTrue(suggestions.contains("Avahi"))
        assertTrue(suggestions.contains("stale _matterc._udp records"))
        assertTrue(suggestions.contains("pairing window again"))
    }
}
