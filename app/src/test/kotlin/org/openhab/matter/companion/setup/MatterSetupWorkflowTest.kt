package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatterSetupWorkflowTest {
    @Test
    fun automatedFlowSucceedsWhenInboxEntryIsDetected() {
        val ports = FakeMatterSetupPorts()
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:TEST")

        assertEquals(MatterSetupStage.SuccessInboxDetected, states.last().stage)
        assertEquals("Device found by openHAB", states.last().title)
        assertTrue(ports.loadConfigCalled)
        assertTrue(ports.readinessCalled)
        assertTrue(ports.commissionCalled)
        assertTrue(ports.openWindowCalled)
        assertTrue(ports.scanCalled)
        assertTrue(ports.inboxCalled)
        assertEquals(
            listOf(
                MatterSetupStage.ReadinessChecking,
                MatterSetupStage.CommissioningToPhone,
                MatterSetupStage.OpeningCommissioningWindow,
                MatterSetupStage.CommissioningWindowOpen,
                MatterSetupStage.SendingCodeToOpenHab,
                MatterSetupStage.WatchingOpenHabInbox,
                MatterSetupStage.SuccessInboxDetected
            ),
            states.map { it.stage }
        )
        assertFalse(states.joinToString("\n").contains("34970112332"))
    }

    private class FakeMatterSetupPorts : MatterSetupPorts {
        var loadConfigCalled = false
        var readinessCalled = false
        var commissionCalled = false
        var openWindowCalled = false
        var scanCalled = false
        var inboxCalled = false

        override fun loadConfig(): MatterSetupConfig {
            loadConfigCalled = true
            return MatterSetupConfig(
                openHabBaseUrl = "http://openhab.local:8080",
                openHabApiToken = "ohab_secret",
                threadDataset = "hex:001122",
                otbrBaseUrl = "http://otbr.local",
                attestationBypassEnabled = false
            )
        }

        override fun checkReadiness(config: MatterSetupConfig): MatterSetupPorts.ReadinessResult {
            readinessCalled = true
            return MatterSetupPorts.ReadinessResult(ready = true, details = listOf("openHAB connected"))
        }

        override fun commissionToPhone(
            setupPayload: String,
            config: MatterSetupConfig
        ): MatterSetupPorts.CommissionResult {
            commissionCalled = true
            return MatterSetupPorts.CommissionResult(nodeId = 1234L, controllerState = "controller-state")
        }

        override fun openCommissioningWindow(
            nodeId: Long,
            controllerState: String
        ): MatterSetupPorts.OpenWindowResult {
            openWindowCalled = true
            return MatterSetupPorts.OpenWindowResult(
                manualCode = "34970112332",
                qrCode = "",
                controllerState = "controller-state-2",
                timeoutSeconds = 300
            )
        }

        override fun sendCodeToOpenHab(
            manualCode: String,
            config: MatterSetupConfig
        ): MatterSetupPorts.OpenHabScanResult {
            scanCalled = true
            return MatterSetupPorts.OpenHabScanResult(started = true, timeoutSeconds = 120, details = "scan accepted")
        }

        override fun waitForOpenHabInbox(
            config: MatterSetupConfig,
            timeoutSeconds: Int
        ): MatterSetupPorts.InboxResult {
            inboxCalled = true
            return MatterSetupPorts.InboxResult(matterEntryDetected = true, details = "Matter Inbox entry detected")
        }

        override fun runDiagnostics(
            failure: MatterSetupFailure,
            config: MatterSetupConfig
        ): MatterSetupDiagnosticsSummary {
            return MatterSetupDiagnosticsSummary.empty()
        }
    }
}
