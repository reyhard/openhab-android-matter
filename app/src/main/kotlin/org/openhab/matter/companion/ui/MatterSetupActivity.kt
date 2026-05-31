package org.openhab.matter.companion.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
    private var pendingSetupAction: MatterSetupAction? = null

    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleQrScanResult(result.data)
        }
    }

    private val setupPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        continuePendingSetupActionAfterPermissions()
    }

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshScanReadiness()
        continuePendingSetupActionAfterLocationSettings()
    }

    private val bluetoothSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshScanReadiness()
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
                openHabTokenStored = viewModel.openHabTokenStored,
                threadDataset = viewModel.threadDataset,
                otbrBaseUrl = viewModel.otbrBaseUrl,
                attestationBypassEnabled = viewModel.attestationBypassEnabled,
                threadSettingsMessage = viewModel.threadSettingsMessage,
                threadNetworkState = viewModel.threadNetworkState,
                openHabConnectionState = viewModel.openHabConnectionState,
                threadBorderRouters = viewModel.threadBorderRouters,
                threadBorderRouterDiscoveryInProgress = viewModel.threadBorderRouterDiscoveryInProgress,
                phoneDevices = viewModel.phoneDevices,
                scanReadiness = viewModel.scanReadiness,
                ipv6DiagnosticAddress = viewModel.ipv6DiagnosticAddress,
                manualSetupCode = viewModel.manualSetupCode,
                onOpenHabUrlChange = viewModel::onOpenHabUrlChange,
                onTokenChange = viewModel::onTokenChange,
                onThreadDatasetChange = viewModel::onThreadDatasetChange,
                onOtbrBaseUrlChange = viewModel::onOtbrBaseUrlChange,
                onAttestationBypassChange = viewModel::onAttestationBypassChange,
                onIpv6DiagnosticAddressChange = viewModel::onIpv6DiagnosticAddressChange,
                onManualSetupCodeChange = viewModel::onManualSetupCodeChange,
                onAction = ::handleAction
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshScanReadiness()
    }

    private fun handleAction(action: MatterSetupAction) {
        when (action) {
            MatterSetupAction.StartScan -> {
                viewModel.handleAction(action)
                qrScannerLauncher.launch(Intent(this, InAppQrScannerActivity::class.java))
            }

            MatterSetupAction.ConfirmPairingMode -> {
                handleConfirmPairingMode()
            }

            MatterSetupAction.RequestSetupPermissions -> {
                val missingPermissions = MatterSetupRuntimePermissions.missingForSetup(this)
                if (missingPermissions.isEmpty()) {
                    viewModel.refreshScanReadiness()
                } else {
                    setupPermissionLauncher.launch(missingPermissions.toTypedArray())
                }
            }

            MatterSetupAction.OpenLocationSettings -> {
                locationSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }

            MatterSetupAction.OpenBluetoothSettings -> {
                bluetoothSettingsLauncher.launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }

            else -> {
                viewModel.handleAction(action)
            }
        }
    }

    private fun handleConfirmPairingMode() {
        val missingPermissions = MatterSetupRuntimePermissions.missingForSetup(this)
        if (missingPermissions.isNotEmpty()) {
            pendingSetupAction = MatterSetupAction.ConfirmPairingMode
            setupPermissionLauncher.launch(missingPermissions.toTypedArray())
            return
        }
        if (!MatterSetupRuntimePermissions.locationServicesEnabled(this)) {
            pendingSetupAction = MatterSetupAction.ConfirmPairingMode
            locationSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }
        viewModel.handleAction(MatterSetupAction.ConfirmPairingMode)
    }

    private fun continuePendingSetupActionAfterPermissions() {
        viewModel.refreshScanReadiness()
        if (pendingSetupAction != MatterSetupAction.ConfirmPairingMode) {
            return
        }
        val missingPermissions = MatterSetupRuntimePermissions.missingForSetup(this)
        if (missingPermissions.isNotEmpty()) {
            return
        }
        if (!MatterSetupRuntimePermissions.locationServicesEnabled(this)) {
            locationSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }
        dispatchPendingSetupAction()
    }

    private fun continuePendingSetupActionAfterLocationSettings() {
        if (pendingSetupAction != MatterSetupAction.ConfirmPairingMode) {
            return
        }
        if (MatterSetupRuntimePermissions.missingForSetup(this).isEmpty() &&
            MatterSetupRuntimePermissions.locationServicesEnabled(this)
        ) {
            dispatchPendingSetupAction()
        } else {
            pendingSetupAction = null
        }
    }

    private fun dispatchPendingSetupAction() {
        val action = pendingSetupAction ?: return
        pendingSetupAction = null
        viewModel.handleAction(action)
    }

    private fun handleQrScanResult(data: Intent?) {
        val payload = QrScanIntentFactory.extractMatterSetupPayload(data)
        viewModel.onQrPayloadScanned(payload)
    }
}
