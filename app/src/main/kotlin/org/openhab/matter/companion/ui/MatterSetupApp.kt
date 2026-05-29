package org.openhab.matter.companion.ui

import androidx.compose.runtime.Composable
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupStage
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.theme.MatterSetupTheme

@Composable
fun MatterSetupApp(
    state: MatterSetupUiState,
    openHabUrl: String,
    token: String,
    onOpenHabUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupTheme {
        when (state.stage) {
            MatterSetupStage.NeedsOpenHabSetup,
            MatterSetupStage.OpenHabSetupChecking -> OpenHabSetupScreen(
                state = state,
                openHabUrl = openHabUrl,
                token = token,
                onUrlChange = onOpenHabUrlChange,
                onTokenChange = onTokenChange,
                onAction = onAction
            )

            MatterSetupStage.ReadyToScan,
            MatterSetupStage.ScanningQr,
            MatterSetupStage.QrScanned -> ScanDeviceScreen(
                state = state,
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

            MatterSetupStage.AdvancedTroubleshooting -> AdvancedTroubleshootingScreen(
                state = state,
                onAction = onAction
            )
        }
    }
}
