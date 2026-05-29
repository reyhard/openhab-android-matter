package org.openhab.matter.companion.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import org.openhab.matter.companion.diagnostics.ThreadBorderRouterRecord
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupBackNavigation
import org.openhab.matter.companion.setup.MatterSetupStage
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.setup.PhoneMatterDevice
import org.openhab.matter.companion.ui.theme.MatterSetupTheme

@Composable
fun MatterSetupApp(
    state: MatterSetupUiState,
    openHabUrl: String,
    token: String,
    threadDataset: String,
    otbrBaseUrl: String,
    attestationBypassEnabled: Boolean,
    threadSettingsMessage: String,
    threadBorderRouters: List<ThreadBorderRouterRecord>,
    threadBorderRouterDiscoveryInProgress: Boolean,
    phoneDevices: List<PhoneMatterDevice>,
    ipv6DiagnosticAddress: String,
    manualSetupCode: String,
    onOpenHabUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onThreadDatasetChange: (String) -> Unit,
    onOtbrBaseUrlChange: (String) -> Unit,
    onAttestationBypassChange: (Boolean) -> Unit,
    onIpv6DiagnosticAddressChange: (String) -> Unit,
    onManualSetupCodeChange: (String) -> Unit,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupTheme {
        val systemBackAction = MatterSetupBackNavigation.systemBackAction(state)
        BackHandler(enabled = systemBackAction != null) {
            systemBackAction?.let(onAction)
        }
        when (state.stage) {
            MatterSetupStage.Welcome,
            MatterSetupStage.NeedsOpenHabSetup,
            MatterSetupStage.OpenHabSetupChecking,
            MatterSetupStage.Settings,
            MatterSetupStage.ChangeToken,
            MatterSetupStage.ThreadNetworkEditor -> OpenHabSetupScreen(
                state = state,
                openHabUrl = openHabUrl,
                token = token,
                threadDataset = threadDataset,
                otbrBaseUrl = otbrBaseUrl,
                attestationBypassEnabled = attestationBypassEnabled,
                threadSettingsMessage = threadSettingsMessage,
                threadBorderRouters = threadBorderRouters,
                threadBorderRouterDiscoveryInProgress = threadBorderRouterDiscoveryInProgress,
                onUrlChange = onOpenHabUrlChange,
                onTokenChange = onTokenChange,
                onThreadDatasetChange = onThreadDatasetChange,
                onOtbrBaseUrlChange = onOtbrBaseUrlChange,
                onAttestationBypassChange = onAttestationBypassChange,
                onAction = onAction
            )

            MatterSetupStage.ReadyToScan,
            MatterSetupStage.ScanningQr,
            MatterSetupStage.QrScanned -> ScanDeviceScreen(
                state = state,
                onAction = onAction
            )

            MatterSetupStage.EnteringManualCode -> ManualCodeScreen(
                state = state,
                manualSetupCode = manualSetupCode,
                onManualSetupCodeChange = onManualSetupCodeChange,
                onAction = onAction
            )

            MatterSetupStage.NeedsPairingMode -> PairingModeScreen(
                state = state,
                onAction = onAction
            )

            MatterSetupStage.ReadinessChecking,
            MatterSetupStage.CommissioningToPhone,
            MatterSetupStage.OpeningCommissioningWindow,
            MatterSetupStage.CommissioningWindowOpen,
            MatterSetupStage.SendingCodeToOpenHab,
            MatterSetupStage.WatchingOpenHabInbox -> SetupProgressScreen(
                state = state,
                onAction = onAction
            )

            MatterSetupStage.SuccessInboxDetected -> SetupSuccessScreen(
                state = state,
                onAction = onAction
            )

            MatterSetupStage.Failed -> SetupFailureScreen(
                state = state,
                onAction = onAction
            )

            MatterSetupStage.PhoneDeviceList -> PhoneDeviceListScreen(
                state = state,
                devices = phoneDevices,
                onAction = onAction
            )

            MatterSetupStage.AdvancedTroubleshooting -> AdvancedTroubleshootingScreen(
                state = state,
                ipv6DiagnosticAddress = ipv6DiagnosticAddress,
                onIpv6DiagnosticAddressChange = onIpv6DiagnosticAddressChange,
                onAction = onAction
            )
        }
    }
}
