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
        assertEquals("MT:TEST", ports.commissionSetupPayload)
        assertEquals(1234L, ports.openWindowNodeId)
        assertEquals("controller-state", ports.openWindowControllerState)
        assertEquals("34970112332", ports.scanManualCode)
        assertEquals(120, ports.waitForInboxTimeoutSeconds)
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
    fun automatedFlowIgnoresMatterInboxEntriesThatExistedBeforeOpenHabScan() {
        val staleEntryId = "matter:node:controller:old"
        val ports = FakeMatterSetupPorts().apply {
            baselineMatterEntryIds = setOf(staleEntryId)
            inboxMatterEntryIds = setOf(staleEntryId)
            inboxMatterEntryDetected = true
            inboxDetails = "Only stale Matter Inbox entry detected"
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:TEST")

        assertTrue(ports.baselineInboxCalled)
        assertEquals(setOf(staleEntryId), ports.waitForInboxBaselineMatterEntryIds)
        assertEquals(MatterSetupStage.Failed, states.last().stage)
        assertEquals(MatterSetupStage.WatchingOpenHabInbox, states.last().failure?.step)
        assertEquals("openHAB did not report the device yet", states.last().failure?.message)
    }

    @Test
    fun openPairingWindowStatesIncludeCommissionedDeviceIdentity() {
        val ports = FakeMatterSetupPorts().apply {
            vendorName = "Aqara"
            productName = "U200"
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("pin=20202021;disc=3840;vendor=Aqara;product=U200")

        val windowOpenState = states.first { it.stage == MatterSetupStage.CommissioningWindowOpen }
        val sendingState = states.first { it.stage == MatterSetupStage.SendingCodeToOpenHab }
        val watchingState = states.first { it.stage == MatterSetupStage.WatchingOpenHabInbox }
        assertEquals("Aqara", windowOpenState.deviceIdentity?.vendorName)
        assertEquals("U200", windowOpenState.deviceIdentity?.productName)
        assertEquals("Aqara U200", windowOpenState.deviceIdentity?.displayName)
        assertEquals(windowOpenState.deviceIdentity, sendingState.deviceIdentity)
        assertEquals(windowOpenState.deviceIdentity, watchingState.deviceIdentity)
    }

    @Test
    fun commissioningProgressUpdatesCurrentPhoneCommissioningDetail() {
        val ports = FakeMatterSetupPorts().apply {
            commissionProgressMessages = listOf(
                "Matter BLE scan round 1 of 2 for discriminator 3840",
                "Bluetooth GATT connected; discovering Matter services",
                "Device attestation completed; continuing commissioning with attestation bypass disabled",
                "connectedhomeip commissioning stage started: FindOperational"
            )
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:TEST")

        val commissioningDetails = states
            .filter { it.stage == MatterSetupStage.CommissioningToPhone }
            .mapNotNull { it.activeDetail }
        assertEquals(
            listOf(
                "Seeking Bluetooth device",
                "Sending setup data over BLE",
                "Connecting device to Thread network",
                "Waiting for device on the Thread network"
            ),
            commissioningDetails
        )
    }

    @Test
    fun readinessFailureIncludesWarningsThenDetailsAndOmitsBlanks() {
        val ports = FakeMatterSetupPorts().apply {
            readinessReady = false
            readinessWarnings = listOf("native runtime unavailable", " ")
            readinessDetails = listOf("", "connectedhomeip artifact missing")
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:TEST")

        val failure = states.last().failure!!
        assertEquals(MatterSetupStage.ReadinessChecking, failure.step)
        assertEquals(
            "native runtime unavailable; connectedhomeip artifact missing",
            failure.details
        )
        assertFalse(ports.commissionCalled)
    }

    @Test
    fun scanFailureRedactsSecretsInFailureStateAndDiagnostics() {
        val ports = FakeMatterSetupPorts().apply {
            scanStarted = false
            scanDetails =
                "HTTP 401 while scanning 34970112332 with ohab_secret hex:001122 MT:SECRET controller-state controller-state-2"
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
        assertEquals(MatterSetupStage.Failed, state.stage)
        assertEquals(MatterSetupStage.SendingCodeToOpenHab, failure.step)
        assertTrue(failure.suggestions.contains("Check the openHAB address and access token."))
        assertTrue(ports.diagnosticsCalled)
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
        assertFalse(state.toString().contains("ohab_secret"))
        assertFalse(state.toString().contains("34970112332"))
        assertEquals("http://openhab.local:8080", ports.diagnosticsContext!!.openHabBaseUrl)
        assertEquals("http://otbr.local", ports.diagnosticsContext!!.otbrBaseUrl)
        assertFalse(ports.diagnosticsContext!!.attestationBypassEnabled)
        assertFalse(ports.diagnosticsContext!!.toString().contains("ohab_secret"))
        assertFalse(ports.diagnosticsContext!!.toString().contains("hex:001122"))
    }

    @Test
    fun scanFailureReplacesRawConfiguredUrlsWithSafeUrlsInFailureStateAndDiagnostics() {
        val ports = FakeMatterSetupPorts().apply {
            openHabBaseUrl =
                "https://openhab-user:openhab-password@openhab.local:8443/rest?token=ohab_secret&querySecret=scan-query#scan-fragment"
            otbrBaseUrl =
                "http://otbr-user:otbr-password@otbr.local/api?dataset=hex:001122&querySecret=otbr-query#otbr-fragment"
            scanStarted = false
            scanDetails = "scan failed at $openHabBaseUrl through $otbrBaseUrl with manual 34970112332"
            diagnosticsSummary = MatterSetupDiagnosticsSummary(
                checks = listOf("check $openHabBaseUrl"),
                warnings = listOf("warning $otbrBaseUrl"),
                details = listOf("detail $openHabBaseUrl $otbrBaseUrl MT:SECRET")
            )
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:SECRET")

        val failureDetails = states.last().failure!!.details
        val diagnosticsText = states.last().diagnostics.toString()
        assertTrue(failureDetails.contains("https://openhab.local:8443/rest"))
        assertTrue(failureDetails.contains("http://otbr.local/api"))
        assertTrue(diagnosticsText.contains("https://openhab.local:8443/rest"))
        assertTrue(diagnosticsText.contains("http://otbr.local/api"))
        assertFalse(failureDetails.contains("openhab-user"))
        assertFalse(failureDetails.contains("openhab-password"))
        assertFalse(failureDetails.contains("otbr-user"))
        assertFalse(failureDetails.contains("otbr-password"))
        assertFalse(failureDetails.contains("querySecret"))
        assertFalse(failureDetails.contains("scan-query"))
        assertFalse(failureDetails.contains("otbr-query"))
        assertFalse(failureDetails.contains("scan-fragment"))
        assertFalse(failureDetails.contains("otbr-fragment"))
        assertFalse(failureDetails.contains("ohab_secret"))
        assertFalse(failureDetails.contains("hex:001122"))
        assertFalse(failureDetails.contains("34970112332"))
        assertFalse(diagnosticsText.contains("openhab-user"))
        assertFalse(diagnosticsText.contains("openhab-password"))
        assertFalse(diagnosticsText.contains("otbr-user"))
        assertFalse(diagnosticsText.contains("otbr-password"))
        assertFalse(diagnosticsText.contains("querySecret"))
        assertFalse(diagnosticsText.contains("scan-query"))
        assertFalse(diagnosticsText.contains("otbr-query"))
        assertFalse(diagnosticsText.contains("scan-fragment"))
        assertFalse(diagnosticsText.contains("otbr-fragment"))
        assertFalse(diagnosticsText.contains("ohab_secret"))
        assertFalse(diagnosticsText.contains("hex:001122"))
        assertFalse(diagnosticsText.contains("34970112332"))
    }

    @Test
    fun scanFailureSanitizesDerivedUrlsInFailureStateAndDiagnostics() {
        val ports = FakeMatterSetupPorts().apply {
            openHabBaseUrl =
                "https://openhab-user:openhab-password@openhab.local:8443/rest?token=ohab_secret"
            otbrBaseUrl =
                "http://otbr-user:otbr-password@otbr.local/api?dataset=hex:001122"
            scanStarted = false
            scanDetails =
                "scan failed at https://derived-user:derived-password@openhab.local:8443/rest/things?querySecret=value&token=ohab_secret#scan-fragment with manual 34970112332 and payload MT:SECRET"
            diagnosticsSummary = MatterSetupDiagnosticsSummary(
                checks = listOf(
                    "check http://derived-otbr:derived-otbr-password@otbr.local/api/thread-dataset?dataset=hex:001122&querySecret=otbr-value#otbr-fragment"
                ),
                warnings = listOf("warning controller-state"),
                details = listOf(
                    "detail https://derived-user:derived-password@openhab.local:8443/rest/things/123?manual=34970112332#detail-fragment MT:SECRET controller-state-2"
                )
            )
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:SECRET")

        val failureDetails = states.last().failure!!.details
        val diagnosticsText = states.last().diagnostics.toString()
        assertTrue(failureDetails.contains("https://openhab.local:8443/rest/things"))
        assertTrue(diagnosticsText.contains("http://otbr.local/api/thread-dataset"))
        assertTrue(diagnosticsText.contains("https://openhab.local:8443/rest/things/123"))
        assertFalse(failureDetails.contains("derived-user"))
        assertFalse(failureDetails.contains("derived-password"))
        assertFalse(failureDetails.contains("querySecret"))
        assertFalse(failureDetails.contains("value"))
        assertFalse(failureDetails.contains("scan-fragment"))
        assertFalse(failureDetails.contains("ohab_secret"))
        assertFalse(failureDetails.contains("hex:001122"))
        assertFalse(failureDetails.contains("34970112332"))
        assertFalse(failureDetails.contains("MT:SECRET"))
        assertFalse(failureDetails.contains("controller-state"))
        assertFalse(failureDetails.contains("controller-state-2"))
        assertFalse(diagnosticsText.contains("derived-user"))
        assertFalse(diagnosticsText.contains("derived-password"))
        assertFalse(diagnosticsText.contains("derived-otbr"))
        assertFalse(diagnosticsText.contains("derived-otbr-password"))
        assertFalse(diagnosticsText.contains("manual="))
        assertFalse(diagnosticsText.contains("querySecret"))
        assertFalse(diagnosticsText.contains("otbr-value"))
        assertFalse(diagnosticsText.contains("detail-fragment"))
        assertFalse(diagnosticsText.contains("otbr-fragment"))
        assertFalse(diagnosticsText.contains("ohab_secret"))
        assertFalse(diagnosticsText.contains("hex:001122"))
        assertFalse(diagnosticsText.contains("34970112332"))
        assertFalse(diagnosticsText.contains("MT:SECRET"))
        assertFalse(diagnosticsText.contains("controller-state"))
        assertFalse(diagnosticsText.contains("controller-state-2"))
    }

    @Test
    fun readinessFailureReplacesRawConfiguredUrlsWithSafeUrlsInFailureDetails() {
        val ports = FakeMatterSetupPorts().apply {
            openHabBaseUrl =
                "https://ready-user:ready-password@openhab.local:8443/rest?token=ohab_secret&querySecret=ready-query#ready-fragment"
            otbrBaseUrl =
                "http://otbr-ready:otbr-ready-password@otbr.local/api?dataset=hex:001122&querySecret=otbr-ready-query#otbr-ready-fragment"
            readinessReady = false
            readinessWarnings = listOf("openHAB readiness failed at $openHabBaseUrl")
            readinessDetails = listOf("OTBR readiness failed at $otbrBaseUrl")
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:SECRET")

        val failureDetails = states.last().failure!!.details
        assertTrue(failureDetails.contains("https://openhab.local:8443/rest"))
        assertTrue(failureDetails.contains("http://otbr.local/api"))
        assertFalse(failureDetails.contains("ready-user"))
        assertFalse(failureDetails.contains("ready-password"))
        assertFalse(failureDetails.contains("otbr-ready"))
        assertFalse(failureDetails.contains("otbr-ready-password"))
        assertFalse(failureDetails.contains("querySecret"))
        assertFalse(failureDetails.contains("ready-query"))
        assertFalse(failureDetails.contains("otbr-ready-query"))
        assertFalse(failureDetails.contains("ready-fragment"))
        assertFalse(failureDetails.contains("otbr-ready-fragment"))
        assertFalse(failureDetails.contains("ohab_secret"))
        assertFalse(failureDetails.contains("hex:001122"))
    }

    @Test
    fun setupResultToStringRedactsSensitiveValues() {
        val commission = MatterSetupPorts.CommissionResult(
            nodeId = 1234L,
            controllerState = "controller-state-secret"
        )
        val window = MatterSetupPorts.OpenWindowResult(
            manualCode = "34970112332",
            qrCode = "MT:QRSECRET",
            controllerState = "controller-state-2-secret",
            timeoutSeconds = 300
        )
        val scan = MatterSetupPorts.OpenHabScanResult(
            started = true,
            timeoutSeconds = 120,
            details = "manual 34970112332 token ohab_secret dataset hex:001122 response body"
        )
        val inbox = MatterSetupPorts.InboxResult(
            matterEntryDetected = true,
            details = "inbox payload MT:QRSECRET token ohab_secret raw openHAB response"
        )

        val commissionText = commission.toString()
        val windowText = window.toString()
        val scanText = scan.toString()
        val inboxText = inbox.toString()

        assertTrue(commissionText.contains("nodeId=1234"))
        assertTrue(commissionText.contains("controllerState=<redacted>"))
        assertFalse(commissionText.contains("controller-state-secret"))
        assertTrue(windowText.contains("manualCode=<redacted>"))
        assertTrue(windowText.contains("qrCode=<redacted>"))
        assertTrue(windowText.contains("controllerState=<redacted>"))
        assertTrue(windowText.contains("timeoutSeconds=300"))
        assertFalse(windowText.contains("34970112332"))
        assertFalse(windowText.contains("MT:QRSECRET"))
        assertFalse(windowText.contains("controller-state-2-secret"))
        assertTrue(scanText.contains("started=true"))
        assertTrue(scanText.contains("timeoutSeconds=120"))
        assertTrue(scanText.contains("details=<redacted>"))
        assertFalse(scanText.contains("34970112332"))
        assertFalse(scanText.contains("ohab_secret"))
        assertFalse(scanText.contains("hex:001122"))
        assertFalse(scanText.contains("response body"))
        assertTrue(inboxText.contains("matterEntryDetected=true"))
        assertTrue(inboxText.contains("details=<redacted>"))
        assertFalse(inboxText.contains("MT:QRSECRET"))
        assertFalse(inboxText.contains("ohab_secret"))
        assertFalse(inboxText.contains("raw openHAB response"))
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
        assertEquals("", ports.diagnosticsContext!!.openHabBaseUrl)
        assertEquals("", ports.diagnosticsContext!!.otbrBaseUrl)
        assertFalse(ports.diagnosticsContext!!.attestationBypassEnabled)
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
    fun diagnosticsContextUrlsAreSafeBeforeDiagnosticsCanEchoThem() {
        val ports = FakeMatterSetupPorts().apply {
            openHabBaseUrl = "https://user:password@openhab.local:8443/rest?token=ohab_secret&code=34970112332"
            otbrBaseUrl = "http://otbr-user:otbr-password@otbr.local/api?dataset=hex:001122"
            scanStarted = false
            echoDiagnosticsContext = true
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:SECRET")

        val diagnosticsText = states.last().diagnostics.toString()
        assertEquals("https://openhab.local:8443/rest", ports.diagnosticsContext!!.openHabBaseUrl)
        assertEquals("http://otbr.local/api", ports.diagnosticsContext!!.otbrBaseUrl)
        assertTrue(diagnosticsText.contains("openHAB https://openhab.local:8443/rest"))
        assertTrue(diagnosticsText.contains("OTBR http://otbr.local/api"))
        assertFalse(diagnosticsText.contains("user:password"))
        assertFalse(diagnosticsText.contains("otbr-user:otbr-password"))
        assertFalse(diagnosticsText.contains("password"))
        assertFalse(diagnosticsText.contains("otbr-password"))
        assertFalse(diagnosticsText.contains("token="))
        assertFalse(diagnosticsText.contains("code="))
        assertFalse(diagnosticsText.contains("dataset="))
        assertFalse(diagnosticsText.contains("ohab_secret"))
        assertFalse(diagnosticsText.contains("hex:001122"))
        assertFalse(diagnosticsText.contains("34970112332"))
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
    fun inboxFailureFailsAtInboxStepAndSanitizesDetailsAndDiagnostics() {
        val ports = FakeMatterSetupPorts().apply {
            qrCode = "MT:QRSECRET"
            inboxMatterEntryDetected = false
            inboxDetails =
                "No Matter Inbox entry before timeout for 34970112332 MT:SECRET ohab_secret hex:001122 controller-state controller-state-2 MT:QRSECRET"
            diagnosticsSummary = MatterSetupDiagnosticsSummary(
                checks = listOf("inbox check MT:SECRET"),
                warnings = listOf("manual 34970112332 qr MT:QRSECRET"),
                details = listOf("token ohab_secret dataset hex:001122 controller-state-2")
            )
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:SECRET")

        val state = states.last()
        val failure = state.failure!!
        val diagnosticsText = state.diagnostics.toString()
        assertEquals(MatterSetupStage.Failed, state.stage)
        assertEquals(MatterSetupStage.WatchingOpenHabInbox, failure.step)
        assertEquals("openHAB did not report the device yet", failure.message)
        assertTrue(ports.inboxCalled)
        assertEquals(failure, ports.diagnosticsFailure)
        assertTrue(ports.diagnosticsCalled)
        assertEquals("http://openhab.local:8080", ports.diagnosticsContext!!.openHabBaseUrl)
        assertTrue(failure.suggestions.contains("Check IPv6 routing between openHAB and the Thread network."))
        assertTrue(failure.suggestions.contains("Check OTBR reachability from openHAB."))
        assertTrue(failure.suggestions.contains("Check mDNS or Avahi on the openHAB side, including stale _matterc._udp records."))
        assertFalse(states.any { it.stage == MatterSetupStage.SuccessInboxDetected })
        assertFalse(failure.details.contains("34970112332"))
        assertFalse(failure.details.contains("MT:SECRET"))
        assertFalse(failure.details.contains("ohab_secret"))
        assertFalse(failure.details.contains("hex:001122"))
        assertFalse(failure.details.contains("controller-state"))
        assertFalse(failure.details.contains("controller-state-2"))
        assertFalse(failure.details.contains("MT:QRSECRET"))
        assertFalse(diagnosticsText.contains("34970112332"))
        assertFalse(diagnosticsText.contains("MT:SECRET"))
        assertFalse(diagnosticsText.contains("ohab_secret"))
        assertFalse(diagnosticsText.contains("hex:001122"))
        assertFalse(diagnosticsText.contains("controller-state"))
        assertFalse(diagnosticsText.contains("controller-state-2"))
        assertFalse(diagnosticsText.contains("MT:QRSECRET"))
    }

    @Test
    fun configAndDiagnosticsContextToStringUseSafeUrls() {
        val config = MatterSetupConfig(
            openHabBaseUrl = "https://user:password@openhab.local:8443/rest?token=ohab_secret&code=34970112332",
            openHabApiToken = "ohab_secret",
            threadDataset = "hex:001122",
            otbrBaseUrl = "http://otbr-user:otbr-password@otbr.local/api?dataset=hex:001122",
            attestationBypassEnabled = true
        )
        val context = MatterSetupDiagnosticsContext(
            openHabBaseUrl = config.openHabBaseUrl,
            otbrBaseUrl = config.otbrBaseUrl,
            attestationBypassEnabled = config.attestationBypassEnabled
        )

        val configText = config.toString()
        val contextText = context.toString()

        assertTrue(configText.contains("openHabBaseUrl=https://openhab.local:8443/rest"))
        assertTrue(configText.contains("otbrBaseUrl=http://otbr.local/api"))
        assertTrue(configText.contains("openHabApiToken=<redacted>"))
        assertTrue(configText.contains("threadDataset=<redacted>"))
        assertTrue(contextText.contains("openHabBaseUrl=https://openhab.local:8443/rest"))
        assertTrue(contextText.contains("otbrBaseUrl=http://otbr.local/api"))
        assertTrue(contextText.contains("attestationBypassEnabled=true"))
        assertFalse(configText.contains("user:password"))
        assertFalse(configText.contains("otbr-user:otbr-password"))
        assertFalse(configText.contains("token="))
        assertFalse(configText.contains("dataset="))
        assertFalse(configText.contains("ohab_secret"))
        assertFalse(configText.contains("hex:001122"))
        assertFalse(configText.contains("34970112332"))
        assertFalse(contextText.contains("user:password"))
        assertFalse(contextText.contains("otbr-user:otbr-password"))
        assertFalse(contextText.contains("token="))
        assertFalse(contextText.contains("dataset="))
        assertFalse(contextText.contains("ohab_secret"))
        assertFalse(contextText.contains("hex:001122"))
        assertFalse(contextText.contains("34970112332"))
    }

    @Test
    fun readinessResultToStringRedactsDetailsAndWarningsButPreservesReady() {
        val result = MatterSetupPorts.ReadinessResult(
            ready = true,
            details = listOf("connectedhomeip detail ohab_secret hex:001122"),
            warnings = listOf("warning MT:SECRET 34970112332")
        )

        val text = result.toString()

        assertTrue(text.contains("ready=true"))
        assertTrue(text.contains("details=<redacted>"))
        assertTrue(text.contains("warnings=<redacted>"))
        assertFalse(text.contains("ohab_secret"))
        assertFalse(text.contains("hex:001122"))
        assertFalse(text.contains("MT:SECRET"))
        assertFalse(text.contains("34970112332"))
    }

    private class FakeMatterSetupPorts : MatterSetupPorts {
        var loadConfigCalled = false
        var readinessCalled = false
        var commissionCalled = false
        var openWindowCalled = false
        var scanCalled = false
        var inboxCalled = false
        var baselineInboxCalled = false
        var scanStarted = true
        var scanDetails = "scan accepted"
        var scanTimeoutSeconds = 120
        var windowTimeoutSeconds = 300
        var commissionSetupPayload: String? = null
        var openWindowNodeId: Long? = null
        var openWindowControllerState: String? = null
        var scanManualCode: String? = null
        var waitForInboxTimeoutSeconds: Int? = null
        var waitForInboxBaselineMatterEntryIds: Set<String> = emptySet()
        var diagnosticsCalled = false
        var diagnosticsFailure: MatterSetupFailure? = null
        var diagnosticsContext: MatterSetupDiagnosticsContext? = null
        var diagnosticsSummary = MatterSetupDiagnosticsSummary.empty()
        var diagnosticsError: RuntimeException? = null
        var echoDiagnosticsContext = false
        var loadConfigError: RuntimeException? = null
        var openWindowError: RuntimeException? = null
        var openHabBaseUrl = "http://openhab.local:8080"
        var otbrBaseUrl = "http://otbr.local"
        var manualCode = "34970112332"
        var qrCode = ""
        var commissionControllerState = "controller-state"
        var windowControllerState = "controller-state-2"
        var vendorName = ""
        var productName = ""
        var readinessReady = true
        var readinessDetails = listOf("openHAB connected")
        var readinessWarnings = emptyList<String>()
        var inboxMatterEntryDetected = true
        var inboxDetails = "Matter Inbox entry detected"
        var baselineMatterEntryIds = emptySet<String>()
        var inboxMatterEntryIds = setOf("matter:node:controller:device")
        var commissionProgressMessages = emptyList<String>()

        override fun loadConfig(): MatterSetupConfig {
            loadConfigCalled = true
            loadConfigError?.let { throw it }
            return MatterSetupConfig(
                openHabBaseUrl = openHabBaseUrl,
                openHabApiToken = "ohab_secret",
                threadDataset = "hex:001122",
                otbrBaseUrl = otbrBaseUrl,
                attestationBypassEnabled = false
            )
        }

        override fun checkReadiness(config: MatterSetupConfig): MatterSetupPorts.ReadinessResult {
            readinessCalled = true
            return MatterSetupPorts.ReadinessResult(
                ready = readinessReady,
                details = readinessDetails,
                warnings = readinessWarnings
            )
        }

        override fun commissionToPhone(
            setupPayload: String,
            config: MatterSetupConfig,
            progress: (String) -> Unit
        ): MatterSetupPorts.CommissionResult {
            commissionCalled = true
            commissionSetupPayload = setupPayload
            commissionProgressMessages.forEach(progress)
            return MatterSetupPorts.CommissionResult(
                nodeId = 1234L,
                controllerState = commissionControllerState,
                vendorName = vendorName,
                productName = productName
            )
        }

        override fun openCommissioningWindow(
            nodeId: Long,
            controllerState: String
        ): MatterSetupPorts.OpenWindowResult {
            openWindowCalled = true
            openWindowNodeId = nodeId
            openWindowControllerState = controllerState
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
            scanManualCode = manualCode
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
            return waitForOpenHabInbox(config, timeoutSeconds, emptySet())
        }

        override fun waitForOpenHabInbox(
            config: MatterSetupConfig,
            timeoutSeconds: Int,
            baselineMatterEntryIds: Set<String>
        ): MatterSetupPorts.InboxResult {
            inboxCalled = true
            waitForInboxTimeoutSeconds = timeoutSeconds
            waitForInboxBaselineMatterEntryIds = baselineMatterEntryIds
            val newEntryIds = inboxMatterEntryIds - baselineMatterEntryIds
            val detected = if (baselineMatterEntryIds.isEmpty()) {
                inboxMatterEntryDetected
            } else {
                newEntryIds.isNotEmpty()
            }
            return MatterSetupPorts.InboxResult(
                matterEntryDetected = detected,
                details = inboxDetails,
                matterEntryIds = inboxMatterEntryIds
            )
        }

        override fun readOpenHabInbox(config: MatterSetupConfig): MatterSetupPorts.InboxResult {
            baselineInboxCalled = true
            return MatterSetupPorts.InboxResult(
                matterEntryDetected = baselineMatterEntryIds.isNotEmpty(),
                details = "Baseline Matter Inbox entries",
                matterEntryIds = baselineMatterEntryIds
            )
        }

        override fun runDiagnostics(
            failure: MatterSetupFailure,
            context: MatterSetupDiagnosticsContext
        ): MatterSetupDiagnosticsSummary {
            diagnosticsCalled = true
            diagnosticsFailure = failure
            diagnosticsContext = context
            diagnosticsError?.let { throw it }
            if (echoDiagnosticsContext) {
                return MatterSetupDiagnosticsSummary(
                    checks = listOf("openHAB ${context.openHabBaseUrl}"),
                    warnings = emptyList(),
                    details = listOf("OTBR ${context.otbrBaseUrl}")
                )
            }
            return diagnosticsSummary
        }
    }
}
