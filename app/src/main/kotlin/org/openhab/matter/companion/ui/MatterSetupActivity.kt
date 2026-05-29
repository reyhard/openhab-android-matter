package org.openhab.matter.companion.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.openhab.matter.companion.qr.InAppQrScannerActivity
import org.openhab.matter.companion.qr.QrScanIntentFactory
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupDiagnosticsSummary
import org.openhab.matter.companion.setup.MatterSetupFailure
import org.openhab.matter.companion.setup.MatterSetupStage
import org.openhab.matter.companion.setup.MatterSetupUiState

class MatterSetupActivity : ComponentActivity() {
    private var uiState by mutableStateOf(MatterSetupUiState.initial(openHabConfigured = false))
    private var openHabUrl by mutableStateOf("")
    private var token by mutableStateOf("")
    private var scannedPayload by mutableStateOf("")

    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleQrScanResult(result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                uiState = MatterSetupUiState.progress(MatterSetupStage.ReadinessChecking)
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
