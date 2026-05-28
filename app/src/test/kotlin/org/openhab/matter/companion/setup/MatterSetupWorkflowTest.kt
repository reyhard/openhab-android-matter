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

    @Test
    fun scanFailureRedactsSecretsInFailureStateAndDiagnostics() {
        val ports = FakeMatterSetupPorts().apply {
            scanStarted = false
            scanDetails =
                "scan failed for 34970112332 ohab_secret hex:001122 MT:SECRET controller-state controller-state-2"
            diagnosticsSummary = MatterSetupDiagnosticsSummary(
                checks = listOf("check MT:SECRET"),
                warnings = listOf("warning 34970112332 controller-state"),
                details = listOf("detail ohab_secret hex:001122 controller-state-2")
            )
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:SECRET")

        val state = states.last()
        val failure = states.last().failure!!
        assertEquals(MatterSetupStage.SendingCodeToOpenHab, failure.step)
        assertFalse(failure.details.contains("34970112332"))
        assertFalse(failure.details.contains("ohab_secret"))
        assertFalse(failure.details.contains("hex:001122"))
        assertFalse(failure.details.contains("MT:SECRET"))
        assertFalse(failure.details.contains("controller-state"))
        assertFalse(failure.details.contains("controller-state-2"))
        assertEquals(failure, ports.diagnosticsFailure)
        assertFalse(ports.diagnosticsFailure!!.details.contains("34970112332"))
        assertFalse(ports.diagnosticsFailure!!.details.contains("ohab_secret"))
        assertFalse(ports.diagnosticsFailure!!.details.contains("hex:001122"))
        assertFalse(ports.diagnosticsFailure!!.details.contains("MT:SECRET"))
        assertFalse(state.diagnostics.toString().contains("34970112332"))
        assertFalse(state.diagnostics.toString().contains("ohab_secret"))
        assertFalse(state.diagnostics.toString().contains("hex:001122"))
        assertFalse(state.diagnostics.toString().contains("MT:SECRET"))
        assertFalse(state.diagnostics.toString().contains("controller-state"))
        assertFalse(state.diagnostics.toString().contains("controller-state-2"))
        assertEquals("http://openhab.local:8080", ports.diagnosticsConfig!!.openHabBaseUrl)
        assertEquals("<redacted>", ports.diagnosticsConfig!!.openHabApiToken)
        assertEquals("<redacted>", ports.diagnosticsConfig!!.threadDataset)
        assertEquals("http://otbr.local", ports.diagnosticsConfig!!.otbrBaseUrl)
        assertFalse(ports.diagnosticsConfig!!.attestationBypassEnabled)
        assertFalse(ports.diagnosticsConfig!!.toString().contains("ohab_secret"))
        assertFalse(ports.diagnosticsConfig!!.toString().contains("hex:001122"))
    }

    @Test
    fun configLoadExceptionEmitsFailedStateWithSafeDiagnosticsConfig() {
        val ports = FakeMatterSetupPorts().apply {
            loadConfigError = IllegalStateException("config failed for MT:SECRET")
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:SECRET")

        val state = states.last()
        assertEquals(MatterSetupStage.Failed, state.stage)
        assertEquals(MatterSetupStage.ReadyToScan, state.failure!!.step)
        assertFalse(state.failure.details.contains("MT:SECRET"))
        assertEquals(state.failure, ports.diagnosticsFailure)
        assertEquals("", ports.diagnosticsConfig!!.openHabBaseUrl)
        assertEquals("<redacted>", ports.diagnosticsConfig!!.openHabApiToken)
        assertEquals("<redacted>", ports.diagnosticsConfig!!.threadDataset)
        assertEquals("", ports.diagnosticsConfig!!.otbrBaseUrl)
        assertFalse(ports.diagnosticsConfig!!.attestationBypassEnabled)
    }

    @Test
    fun diagnosticsExceptionDoesNotMaskOriginalFailure() {
        val ports = FakeMatterSetupPorts().apply {
            scanStarted = false
            scanDetails = "scan failed for MT:SECRET"
            diagnosticsError = IllegalStateException("diagnostics failed")
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:SECRET")

        val state = states.last()
        assertEquals(MatterSetupStage.Failed, state.stage)
        assertEquals(MatterSetupStage.SendingCodeToOpenHab, state.failure!!.step)
        assertEquals("openHAB could not start pairing", state.failure.message)
        assertFalse(state.failure.details.contains("MT:SECRET"))
        assertEquals(MatterSetupDiagnosticsSummary.empty(), state.diagnostics)
    }

    @Test
    fun diagnosticsReturnIsSanitizedBeforeEmission() {
        val ports = FakeMatterSetupPorts().apply {
            scanStarted = false
            scanDetails = "scan failed"
            qrCode = "MT:QRSECRET"
            diagnosticsSummary = MatterSetupDiagnosticsSummary(
                checks = listOf("payload MT:SECRET"),
                warnings = listOf("manual 34970112332"),
                details = listOf("token ohab_secret dataset hex:001122 qr MT:QRSECRET controller-state-2")
            )
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:SECRET")

        val diagnosticsText = states.last().diagnostics.toString()
        assertFalse(diagnosticsText.contains("MT:SECRET"))
        assertFalse(diagnosticsText.contains("34970112332"))
        assertFalse(diagnosticsText.contains("ohab_secret"))
        assertFalse(diagnosticsText.contains("hex:001122"))
        assertFalse(diagnosticsText.contains("MT:QRSECRET"))
        assertFalse(diagnosticsText.contains("controller-state-2"))
    }

    @Test
    fun blankManualCodeFailsBeforeSendingCodeToOpenHab() {
        val ports = FakeMatterSetupPorts().apply {
            manualCode = "   "
            qrCode = "MT:QRSECRET"
            windowControllerState = "controller-state-2-secret"
            diagnosticsSummary = MatterSetupDiagnosticsSummary(
                checks = listOf("qr MT:QRSECRET"),
                warnings = listOf("manual 34970112332 should not matter"),
                details = listOf("controller controller-state-2-secret")
            )
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:SECRET")

        val state = states.last()
        assertEquals(MatterSetupStage.Failed, state.stage)
        assertEquals(MatterSetupStage.OpeningCommissioningWindow, state.failure!!.step)
        assertEquals("OpenCommissioningWindow did not return a manual setup code", state.failure.message)
        assertFalse(ports.scanCalled)
        assertFalse(state.diagnostics.toString().contains("MT:QRSECRET"))
        assertFalse(state.diagnostics.toString().contains("controller-state-2-secret"))
    }

    @Test
    fun exceptionDuringOpenCommissioningWindowKeepsActiveFailureStep() {
        val ports = FakeMatterSetupPorts().apply {
            openWindowError = IllegalStateException("OCW failed")
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:TEST")

        assertEquals(MatterSetupStage.Failed, states.last().stage)
        assertEquals(MatterSetupStage.OpeningCommissioningWindow, states.last().failure!!.step)
        assertEquals(MatterSetupStage.OpeningCommissioningWindow, ports.diagnosticsFailure!!.step)
    }

    @Test
    fun inboxWaitingStateUsesScanTimeout() {
        val ports = FakeMatterSetupPorts().apply {
            windowTimeoutSeconds = 300
            scanTimeoutSeconds = 120
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:TEST")

        val inboxState = states.first { it.stage == MatterSetupStage.WatchingOpenHabInbox }
        assertEquals(120, inboxState.countdownSeconds)
        assertEquals(120, ports.waitForInboxTimeoutSeconds)
    }

    @Test
    fun configToStringRedactsTokenAndDataset() {
        val config = MatterSetupConfig(
            openHabBaseUrl = "http://openhab.local:8080",
            openHabApiToken = "ohab_secret",
            threadDataset = "hex:001122",
            otbrBaseUrl = "http://otbr.local",
            attestationBypassEnabled = true
        )

        val text = config.toString()

        assertTrue(text.contains("openHabApiToken=<redacted>"))
        assertTrue(text.contains("threadDataset=<redacted>"))
        assertFalse(text.contains("ohab_secret"))
        assertFalse(text.contains("hex:001122"))
    }

    private class FakeMatterSetupPorts : MatterSetupPorts {
        var loadConfigCalled = false
        var readinessCalled = false
        var commissionCalled = false
        var openWindowCalled = false
        var scanCalled = false
        var inboxCalled = false
        var scanStarted = true
        var scanDetails = "scan accepted"
        var scanTimeoutSeconds = 120
        var windowTimeoutSeconds = 300
        var waitForInboxTimeoutSeconds: Int? = null
        var diagnosticsFailure: MatterSetupFailure? = null
        var diagnosticsConfig: MatterSetupConfig? = null
        var diagnosticsSummary = MatterSetupDiagnosticsSummary.empty()
        var diagnosticsError: RuntimeException? = null
        var loadConfigError: RuntimeException? = null
        var openWindowError: RuntimeException? = null
        var manualCode = "34970112332"
        var qrCode = ""
        var commissionControllerState = "controller-state"
        var windowControllerState = "controller-state-2"

        override fun loadConfig(): MatterSetupConfig {
            loadConfigCalled = true
            loadConfigError?.let { throw it }
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
            return MatterSetupPorts.CommissionResult(nodeId = 1234L, controllerState = commissionControllerState)
        }

        override fun openCommissioningWindow(
            nodeId: Long,
            controllerState: String
        ): MatterSetupPorts.OpenWindowResult {
            openWindowCalled = true
            openWindowError?.let { throw it }
            return MatterSetupPorts.OpenWindowResult(
                manualCode = manualCode,
                qrCode = qrCode,
                controllerState = windowControllerState,
                timeoutSeconds = windowTimeoutSeconds
            )
        }

        override fun sendCodeToOpenHab(
            manualCode: String,
            config: MatterSetupConfig
        ): MatterSetupPorts.OpenHabScanResult {
            scanCalled = true
            return MatterSetupPorts.OpenHabScanResult(
                started = scanStarted,
                timeoutSeconds = scanTimeoutSeconds,
                details = scanDetails
            )
        }

        override fun waitForOpenHabInbox(
            config: MatterSetupConfig,
            timeoutSeconds: Int
        ): MatterSetupPorts.InboxResult {
            inboxCalled = true
            waitForInboxTimeoutSeconds = timeoutSeconds
            return MatterSetupPorts.InboxResult(matterEntryDetected = true, details = "Matter Inbox entry detected")
        }

        override fun runDiagnostics(
            failure: MatterSetupFailure,
            config: MatterSetupConfig
        ): MatterSetupDiagnosticsSummary {
            diagnosticsFailure = failure
            diagnosticsConfig = config
            diagnosticsError?.let { throw it }
            return diagnosticsSummary
        }
    }
}
