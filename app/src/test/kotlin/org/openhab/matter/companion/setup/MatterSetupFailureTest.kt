package org.openhab.matter.companion.setup

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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

    @Test
    fun commissioningToPhoneSuggestionsStayBleFocused() {
        val suggestions = MatterSetupFailure.defaultSuggestions(MatterSetupStage.CommissioningToPhone)
        val joined = suggestions.joinToString(" ")

        assertTrue(joined.contains("pairing mode"))
        assertTrue(joined.contains("Bluetooth"))
        assertTrue(joined.contains("Nearby devices"))
        assertTrue(joined.contains("Location"))
        assertFalse(joined.contains("IPv6"))
        assertFalse(joined.contains("mDNS"))
        assertFalse(joined.contains("VPN"))
        assertFalse(joined.contains("routing"))
    }
}
