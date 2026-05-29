package org.openhab.matter.companion.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.openhab.matter.companion.config.SharedPreferencesAppConfigRepository
import org.openhab.matter.companion.controller.ConnectedHomeIpMatterControllerFactory
import org.openhab.matter.companion.controller.FakeMatterController
import org.openhab.matter.companion.controller.MatterBootstrapState
import org.openhab.matter.companion.controller.NativeChipControllerSession
import org.openhab.matter.companion.controller.SharedPreferencesMatterBootstrapStateRepository
import org.openhab.matter.companion.domain.MatterSetupPayloadParser
import org.openhab.matter.companion.domain.ThreadDataset
import org.openhab.matter.companion.openhab.HttpOpenHabClient
import org.openhab.matter.companion.openhab.HttpOpenHabInboxClient
import org.openhab.matter.companion.openhab.HttpOpenHabMatterDiscoveryClient
import org.openhab.matter.companion.openhab.OpenHabInboxStatus
import org.openhab.matter.companion.qr.InAppQrScannerActivity
import org.openhab.matter.companion.qr.QrScanIntentFactory
import org.openhab.matter.companion.setup.AndroidMatterSetupPorts
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupConfig
import org.openhab.matter.companion.setup.MatterSetupDiagnosticsContext
import org.openhab.matter.companion.setup.MatterSetupDiagnosticsSummary
import org.openhab.matter.companion.setup.MatterSetupFailure
import org.openhab.matter.companion.setup.MatterSetupPorts
import org.openhab.matter.companion.setup.MatterSetupStage
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.setup.MatterSetupWorkflow

class MatterSetupActivity : ComponentActivity() {
    private var uiState by mutableStateOf(MatterSetupUiState.initial(openHabConfigured = false))
    private var openHabUrl by mutableStateOf("")
    private var token by mutableStateOf("")
    private var scannedPayload by mutableStateOf("")

    private val configRepository by lazy { SharedPreferencesAppConfigRepository(this) }
    private val bootstrapStateRepository by lazy { SharedPreferencesMatterBootstrapStateRepository(this) }
    private val openHabClient by lazy { HttpOpenHabClient() }
    private val openHabMatterDiscoveryClient by lazy { HttpOpenHabMatterDiscoveryClient() }
    private val openHabInboxClient by lazy { HttpOpenHabInboxClient() }
    private val fakeMatterController by lazy { FakeMatterController() }
    private val controllerSession by lazy {
        newNativeControllerSession(loadMatterSetupConfig().attestationBypassEnabled)
    }

    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleQrScanResult(result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val config = configRepository.load()
        openHabUrl = config.openHabBaseUrl()
        token = config.openHabApiToken()
        uiState = MatterSetupUiState.initial(openHabConfigured = openHabUrl.isNotBlank())

        setContent {
            MatterSetupApp(
                state = uiState,
                openHabUrl = openHabUrl,
                token = token,
                onOpenHabUrlChange = { openHabUrl = it },
                onTokenChange = { token = it },
                onAction = ::handleAction
            )
        }
    }

    private fun handleAction(action: MatterSetupAction) {
        when (action) {
            MatterSetupAction.StartScan -> {
                uiState = scanningState()
                qrScannerLauncher.launch(Intent(this, InAppQrScannerActivity::class.java))
            }

            MatterSetupAction.ConfirmPairingMode -> {
                if (scannedPayload.isBlank()) {
                    uiState = MatterSetupUiState.failed(
                        MatterSetupFailure(
                            step = MatterSetupStage.QrScanned,
                            message = "Scan a Matter QR code before starting setup."
                        ),
                        MatterSetupDiagnosticsSummary.empty()
                    )
                } else {
                    startRealWorkflow(scannedPayload)
                }
            }

            MatterSetupAction.Retry,
            MatterSetupAction.AddAnotherDevice -> {
                scannedPayload = ""
                uiState = MatterSetupUiState.initial(openHabConfigured = openHabUrl.isNotBlank())
            }

            MatterSetupAction.ShowTroubleshooting,
            MatterSetupAction.EnterCodeManually,
            MatterSetupAction.SaveOpenHab,
            MatterSetupAction.TestOpenHab,
            MatterSetupAction.OpenCommissioningWindowAgain,
            MatterSetupAction.ForgetFromPhone -> {
                uiState = advancedTroubleshootingState()
            }
        }
    }

    private fun handleQrScanResult(data: Intent?) {
        val payload = QrScanIntentFactory.extractMatterSetupPayload(data)
        if (payload.isBlank()) {
            uiState = MatterSetupUiState.failed(
                MatterSetupFailure(
                    step = MatterSetupStage.ScanningQr,
                    message = "The QR code was not a valid Matter setup code."
                ),
                MatterSetupDiagnosticsSummary.empty()
            )
            return
        }

        scannedPayload = payload
        uiState = MatterSetupUiState(
            stage = MatterSetupStage.NeedsPairingMode,
            title = "Put device in pairing mode",
            message = "Make sure the Matter device is powered on and ready before continuing.",
            primaryAction = MatterSetupAction.ConfirmPairingMode,
            primaryActionLabel = "Device is ready"
        )
    }

    private fun scanningState(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.ScanningQr,
            title = "Scan Matter QR code",
            message = "Point the camera at the Matter QR code on the device or box.",
            primaryAction = MatterSetupAction.StartScan,
            primaryActionLabel = "Scan QR code"
        )
    }

    private fun startRealWorkflow(setupPayload: String) {
        Thread({
            MatterSetupWorkflow(createRealPorts()) { state ->
                runOnUiThread { uiState = state }
            }.startAutomatedSetup(setupPayload)
        }, "matter-automated-setup").start()
    }

    private fun createRealPorts(): AndroidMatterSetupPorts {
        return AndroidMatterSetupPorts(
            configLoader = ::loadMatterSetupConfig,
            readinessChecker = { baseUrl, apiToken ->
                openHabClient.checkReadiness(baseUrl, apiToken)
            },
            matterRunner = object : AndroidMatterSetupPorts.MatterRunner {
                override fun commissionToPhone(
                    setupPayload: String,
                    config: MatterSetupConfig
                ): MatterSetupPorts.CommissionResult {
                    val dataset = ThreadDataset.parse(config.threadDataset)
                    val payload = MatterSetupPayloadParser.parse(setupPayload)
                    val selection = controllerSession.selectNativeIfReady()
                    if (!selection.nativeSelected()) {
                        throw IllegalStateException("connectedhomeip is not ready for real commissioning.")
                    }

                    val result = selection.controller().commissionBleThread(
                        dataset,
                        payload,
                        bootstrapStateRepository.load().controllerState(),
                        null
                    )
                    bootstrapStateRepository.save(MatterBootstrapState(result.nodeId(), result.controllerState(), false))
                    return MatterSetupPorts.CommissionResult(result.nodeId(), result.controllerState())
                }

                override fun openCommissioningWindow(
                    nodeId: Long,
                    controllerState: String
                ): MatterSetupPorts.OpenWindowResult {
                    val selection = controllerSession.selectNativeIfReady()
                    if (!selection.nativeSelected()) {
                        throw IllegalStateException("connectedhomeip is not ready for Open Commissioning Window.")
                    }

                    val result = selection.controller().openCommissioningWindow(
                        nodeId,
                        300,
                        3840,
                        controllerState,
                        null
                    )
                    bootstrapStateRepository.save(MatterBootstrapState(nodeId, result.controllerState(), false))
                    return MatterSetupPorts.OpenWindowResult(
                        manualCode = result.temporaryCode(),
                        qrCode = result.qrCode(),
                        controllerState = result.controllerState(),
                        timeoutSeconds = 300
                    )
                }
            },
            discoveryScan = { baseUrl, manualCode, apiToken ->
                openHabMatterDiscoveryClient.startMatterScan(baseUrl, manualCode, apiToken)
            },
            inboxWaiter = ::waitForOpenHabInbox,
            diagnosticsRunner = ::runBasicDiagnostics
        )
    }

    private fun loadMatterSetupConfig(): MatterSetupConfig {
        val config = configRepository.load()
        return MatterSetupConfig(
            openHabBaseUrl = openHabUrl,
            openHabApiToken = token,
            threadDataset = config.threadDataset(),
            otbrBaseUrl = config.otbrBaseUrl(),
            attestationBypassEnabled = config.attestationBypassEnabled()
        )
    }

    private fun waitForOpenHabInbox(
        baseUrl: String,
        apiToken: String,
        timeoutSeconds: Int
    ): MatterSetupPorts.InboxResult {
        val deadlineMillis = System.currentTimeMillis() + (timeoutSeconds.coerceAtLeast(30) * 1000L)
        var last = OpenHabInboxStatus(false, false, "openHAB Inbox was not checked", "")
        do {
            last = runCatching {
                openHabInboxClient.checkInbox(baseUrl, apiToken)
            }.getOrElse { error ->
                OpenHabInboxStatus(false, false, "openHAB Inbox check failed", error.message.orEmpty())
            }
            if (last.matterEntryDetected()) {
                break
            }
            val remainingMillis = deadlineMillis - System.currentTimeMillis()
            if (remainingMillis <= 0L) {
                break
            }
            try {
                Thread.sleep(remainingMillis.coerceAtMost(2_000L))
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        } while (true)

        val details = listOf(last.message().orEmpty(), last.details().orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(": ")
        return MatterSetupPorts.InboxResult(last.matterEntryDetected(), details)
    }

    private fun runBasicDiagnostics(
        failure: MatterSetupFailure,
        context: MatterSetupDiagnosticsContext
    ): MatterSetupDiagnosticsSummary {
        return MatterSetupDiagnosticsSummary(
            checks = listOf(
                "openHAB URL configured=${context.openHabBaseUrl.isNotBlank()}",
                "attestation bypass enabled=${context.attestationBypassEnabled}"
            ),
            warnings = listOf(failure.message).filter { it.isNotBlank() },
            details = listOf(
                "Failed at ${failure.step}",
                failure.details.ifBlank { "No additional details." }
            )
        )
    }

    private fun newNativeControllerSession(attestationBypassEnabled: Boolean): NativeChipControllerSession {
        val connectedHomeIpFactory = ConnectedHomeIpMatterControllerFactory(this)
        return NativeChipControllerSession(
            fakeMatterController,
            attestationBypassEnabled,
            connectedHomeIpFactory::create
        )
    }

    private fun advancedTroubleshootingState(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.AdvancedTroubleshooting,
            title = "Advanced troubleshooting",
            message = "This setup action will be connected in a later task.",
            primaryAction = MatterSetupAction.Retry,
            primaryActionLabel = "Back to setup",
            diagnostics = MatterSetupDiagnosticsSummary.empty()
        )
    }
}
