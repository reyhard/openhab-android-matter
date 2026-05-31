package org.openhab.matter.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openhab.matter.companion.otbr.OtbrStatus

class ThreadNetworkUiStateTest {
    @Test
    fun readyWhenDatasetAndBorderRouterAreReachable() {
        val state = ThreadNetworkUiState.fromInputs(
            dataset = "hex:0E080000000000010000",
            otbrBaseUrl = "fd00::1",
            otbrStatus = OtbrStatus(true, "OTBR address is reachable", "")
        )

        assertTrue(state.ready)
        assertEquals(ThreadNetworkStateKind.Ready, state.kind)
        assertEquals("Thread network ready", state.title)
        assertEquals("Ready", state.statusLabel)
    }

    @Test
    fun reportsBorderRouterErrorWhenAddressDoesNotRespond() {
        val state = ThreadNetworkUiState.fromInputs(
            dataset = "hex:0E080000000000010000",
            otbrBaseUrl = "fd00::1",
            otbrStatus = OtbrStatus(false, "OTBR address is not reachable", "timeout")
        )

        assertFalse(state.ready)
        assertEquals(ThreadNetworkStateKind.BorderRouterError, state.kind)
        assertEquals("Check Thread Border Router", state.title)
        assertEquals("Thread Border Router address is not reachable.", state.message)
        assertEquals("Router error", state.statusLabel)
    }

    @Test
    fun reportsDatasetErrorBeforeBorderRouterError() {
        val state = ThreadNetworkUiState.fromInputs(
            dataset = "not-a-dataset",
            otbrBaseUrl = "fd00::1",
            otbrStatus = OtbrStatus(false, "OTBR address is not reachable", "timeout")
        )

        assertFalse(state.ready)
        assertEquals(ThreadNetworkStateKind.DatasetError, state.kind)
        assertEquals("Thread dataset is not valid.", state.title)
    }
}
