package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openhab.matter.companion.openhab.OpenHabMatterDiscoveryScanStatus
import org.openhab.matter.companion.openhab.OpenHabStatus

class AndroidMatterSetupPortsTest {
    @Test
    fun openHabScanFailureDoesNotExposeManualCode() {
        val config = testConfig()
        var scanBaseUrl: String? = null
        var scanManualCode: String? = null
        var scanToken: String? = null
        val ports = AndroidMatterSetupPorts(
            configLoader = { config },
            readinessChecker = { _, _ -> readyStatus() },
            matterRunner = FakeMatterRunner(),
            discoveryScan = { baseUrl, manualCode, token ->
                scanBaseUrl = baseUrl
                scanManualCode = manualCode
                scanToken = token
                OpenHabMatterDiscoveryScanStatus(
                    false,
                    false,
                    "scan failed",
                    "HTTP 401 while scanning 34970112332 with token ohab_secret at " +
                        "https://user:password@openhab.local:8443/rest/discovery?token=ohab_secret#fragment",
                    120
                )
            },
            inboxWaiter = { _, _, _ -> MatterSetupPorts.InboxResult(true, "detected") },
            diagnosticsRunner = { _, _ -> MatterSetupDiagnosticsSummary.empty() }
        )

        val result = ports.sendCodeToOpenHab("34970112332", config)

        assertFalse(result.started)
        assertEquals(120, result.timeoutSeconds)
        assertEquals(config.openHabBaseUrl, scanBaseUrl)
        assertEquals("34970112332", scanManualCode)
        assertEquals(config.openHabApiToken, scanToken)
        assertFalse(result.details.contains("34970112332"))
        assertFalse(result.details.contains("ohab_secret"))
        assertFalse(result.details.contains("user:password"))
        assertFalse(result.details.contains("token="))
        assertFalse(result.details.contains("fragment"))
        assertTrue(result.details.contains("https://openhab.local:8443/rest/discovery"))
        assertFalse(result.toString().contains("34970112332"))
        assertFalse(result.toString().contains("ohab_secret"))
    }

    @Test
    fun readinessFailsWhenOpenHabMatterControllerIsOffline() {
        val config = testConfig()
        val ports = AndroidMatterSetupPorts(
            configLoader = { config },
            readinessChecker = { baseUrl, token ->
                assertEquals(config.openHabBaseUrl, baseUrl)
                assertEquals(config.openHabApiToken, token)
                OpenHabStatus(false, true, false, "Matter controller offline", "Thing status is OFFLINE")
            },
            matterRunner = FakeMatterRunner(),
            discoveryScan = { _, _, _ -> OpenHabMatterDiscoveryScanStatus(true, true, "started", "started", 60) },
            inboxWaiter = { _, _, _ -> MatterSetupPorts.InboxResult(true, "detected") },
            diagnosticsRunner = { _, _ -> MatterSetupDiagnosticsSummary.empty() }
        )

        val result = ports.checkReadiness(config)

        assertFalse(result.ready)
        assertTrue(result.details.contains("Matter controller offline"))
        assertTrue(result.details.contains("Thing status is OFFLINE"))
        assertTrue(result.warnings.contains("Matter controller offline"))
    }

    @Test
    fun delegatesConfigMatterInboxAndDiagnosticsDependencies() {
        val config = testConfig()
        val runner = FakeMatterRunner()
        val failure = MatterSetupFailure(MatterSetupStage.SendingCodeToOpenHab, "failed")
        val context = MatterSetupDiagnosticsContext("http://openhab.local:8080", "http://otbr.local", true)
        val summary = MatterSetupDiagnosticsSummary(
            checks = listOf("check"),
            warnings = listOf("warning"),
            details = listOf("detail")
        )
        var inboxBaseUrl: String? = null
        var inboxToken: String? = null
        var inboxTimeout: Int? = null
        var diagnosticsFailure: MatterSetupFailure? = null
        var diagnosticsContext: MatterSetupDiagnosticsContext? = null
        val ports = AndroidMatterSetupPorts(
            configLoader = { config },
            readinessChecker = { _, _ -> readyStatus() },
            matterRunner = runner,
            discoveryScan = { _, _, _ -> OpenHabMatterDiscoveryScanStatus(true, true, "started", "started", 60) },
            inboxWaiter = { baseUrl, token, timeoutSeconds ->
                inboxBaseUrl = baseUrl
                inboxToken = token
                inboxTimeout = timeoutSeconds
                MatterSetupPorts.InboxResult(false, "not yet")
            },
            diagnosticsRunner = { receivedFailure, receivedContext ->
                diagnosticsFailure = receivedFailure
                diagnosticsContext = receivedContext
                summary
            }
        )

        assertSame(config, ports.loadConfig())
        assertEquals(
            MatterSetupPorts.CommissionResult(1234L, "commissioned-controller-state"),
            ports.commissionToPhone("MT:TEST", config)
        )
        assertEquals("MT:TEST", runner.setupPayload)
        assertSame(config, runner.config)
        assertEquals(
            MatterSetupPorts.OpenWindowResult("34970112332", "MT:QR", "window-controller-state", 300),
            ports.openCommissioningWindow(1234L, "commissioned-controller-state")
        )
        assertEquals(1234L, runner.nodeId)
        assertEquals("commissioned-controller-state", runner.controllerState)
        assertEquals(
            MatterSetupPorts.InboxResult(false, "not yet"),
            ports.waitForOpenHabInbox(config, 90)
        )
        assertEquals(config.openHabBaseUrl, inboxBaseUrl)
        assertEquals(config.openHabApiToken, inboxToken)
        assertEquals(90, inboxTimeout)
        assertSame(summary, ports.runDiagnostics(failure, context))
        assertSame(failure, diagnosticsFailure)
        assertSame(context, diagnosticsContext)
    }

    private fun testConfig(): MatterSetupConfig {
        return MatterSetupConfig(
            openHabBaseUrl = "https://openhab.local:8443/rest",
            openHabApiToken = "ohab_secret",
            threadDataset = "0011223344556677",
            otbrBaseUrl = "http://otbr.local",
            attestationBypassEnabled = false
        )
    }

    private fun readyStatus(): OpenHabStatus {
        return OpenHabStatus(true, true, true, "ready", "ready")
    }

    private class FakeMatterRunner : AndroidMatterSetupPorts.MatterRunner {
        var setupPayload: String? = null
        var config: MatterSetupConfig? = null
        var nodeId: Long? = null
        var controllerState: String? = null

        override fun commissionToPhone(
            setupPayload: String,
            config: MatterSetupConfig
        ): MatterSetupPorts.CommissionResult {
            this.setupPayload = setupPayload
            this.config = config
            return MatterSetupPorts.CommissionResult(1234L, "commissioned-controller-state")
        }

        override fun openCommissioningWindow(
            nodeId: Long,
            controllerState: String
        ): MatterSetupPorts.OpenWindowResult {
            this.nodeId = nodeId
            this.controllerState = controllerState
            return MatterSetupPorts.OpenWindowResult("34970112332", "MT:QR", "window-controller-state", 300)
        }
    }
}
