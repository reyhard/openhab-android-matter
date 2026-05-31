package org.openhab.matter.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openhab.matter.companion.openhab.OpenHabStatus

class OpenHabConnectionUiStateTest {
    @Test
    fun readyStatusShowsConnectedCopy() {
        val state = OpenHabConnectionUiState.fromStatus(
            OpenHabStatus(true, true, true, "openHAB Matter controller is ready", "")
        )

        assertTrue(state.ready)
        assertEquals(OpenHabConnectionStateKind.Connected, state.kind)
        assertEquals("Connected to openHAB", state.title)
        assertEquals("We were able to reach your openHAB home.", state.message)
        assertEquals("Ready", state.statusLabel)
    }

    @Test
    fun unreachableStatusShowsAddressError() {
        val state = OpenHabConnectionUiState.fromStatus(
            OpenHabStatus(false, false, false, "openHAB REST API is not reachable", "timeout")
        )

        assertFalse(state.ready)
        assertEquals(OpenHabConnectionStateKind.AddressError, state.kind)
        assertEquals("Check openHAB address", state.title)
        assertEquals("We could not reach openHAB. Check the address and network.", state.message)
        assertEquals("Address error", state.statusLabel)
    }

    @Test
    fun unauthorizedStatusShowsTokenError() {
        val state = OpenHabConnectionUiState.fromStatus(
            OpenHabStatus(false, true, false, "openHAB access token was rejected", "HTTP 401")
        )

        assertFalse(state.ready)
        assertEquals(OpenHabConnectionStateKind.TokenError, state.kind)
        assertEquals("Check access token", state.title)
        assertEquals("openHAB rejected the access token. Paste a valid API token.", state.message)
        assertEquals("Token error", state.statusLabel)
    }
}
