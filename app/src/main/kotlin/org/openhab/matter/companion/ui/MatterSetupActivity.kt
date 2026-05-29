package org.openhab.matter.companion.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import org.openhab.matter.companion.qr.InAppQrScannerActivity
import org.openhab.matter.companion.qr.QrScanIntentFactory
import org.openhab.matter.companion.setup.MatterSetupAction

class MatterSetupActivity : ComponentActivity() {
    private lateinit var viewModel: MatterSetupViewModel

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

        viewModel = ViewModelProvider(this)[MatterSetupViewModel::class.java]

        setContent {
            MatterSetupApp(
                state = viewModel.uiState,
                openHabUrl = viewModel.openHabUrl,
                token = viewModel.token,
                threadDataset = viewModel.threadDataset,
                otbrBaseUrl = viewModel.otbrBaseUrl,
                attestationBypassEnabled = viewModel.attestationBypassEnabled,
                threadSettingsMessage = viewModel.threadSettingsMessage,
                threadBorderRouters = viewModel.threadBorderRouters,
                threadBorderRouterDiscoveryInProgress = viewModel.threadBorderRouterDiscoveryInProgress,
                phoneDevices = viewModel.phoneDevices,
                ipv6DiagnosticAddress = viewModel.ipv6DiagnosticAddress,
                onOpenHabUrlChange = viewModel::onOpenHabUrlChange,
                onTokenChange = viewModel::onTokenChange,
                onThreadDatasetChange = viewModel::onThreadDatasetChange,
                onOtbrBaseUrlChange = viewModel::onOtbrBaseUrlChange,
                onAttestationBypassChange = viewModel::onAttestationBypassChange,
                onIpv6DiagnosticAddressChange = viewModel::onIpv6DiagnosticAddressChange,
                onAction = ::handleAction
            )
        }
    }

    private fun handleAction(action: MatterSetupAction) {
        if (action == MatterSetupAction.StartScan) {
            viewModel.handleAction(action)
            qrScannerLauncher.launch(Intent(this, InAppQrScannerActivity::class.java))
        } else {
            viewModel.handleAction(action)
        }
    }

    private fun handleQrScanResult(data: Intent?) {
        val payload = QrScanIntentFactory.extractMatterSetupPayload(data)
        viewModel.onQrPayloadScanned(payload)
    }
}
