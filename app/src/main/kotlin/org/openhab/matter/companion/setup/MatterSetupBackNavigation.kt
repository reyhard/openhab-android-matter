package org.openhab.matter.companion.setup

object MatterSetupBackNavigation {
    fun systemBackAction(state: MatterSetupUiState): MatterSetupAction? {
        return when (state.stage) {
            MatterSetupStage.Settings,
            MatterSetupStage.EnteringManualCode,
            MatterSetupStage.ScanningQr,
            MatterSetupStage.NeedsPairingMode -> MatterSetupAction.BackToMainMenu

            MatterSetupStage.OpenHabAddressEditor,
            MatterSetupStage.ChangeToken,
            MatterSetupStage.ThreadNetworkEditor -> MatterSetupAction.BackToSettings

            MatterSetupStage.PhoneDeviceList -> state.primaryAction ?: MatterSetupAction.BackToSettings

            MatterSetupStage.AdvancedTroubleshooting -> state.primaryAction

            MatterSetupStage.NeedsOpenHabSetup,
            MatterSetupStage.OpenHabSetupChecking,
            MatterSetupStage.Welcome,
            MatterSetupStage.ReadyToScan,
            MatterSetupStage.QrScanned,
            MatterSetupStage.ReadinessChecking,
            MatterSetupStage.CommissioningToPhone,
            MatterSetupStage.OpeningCommissioningWindow,
            MatterSetupStage.CommissioningWindowOpen,
            MatterSetupStage.SendingCodeToOpenHab,
            MatterSetupStage.WatchingOpenHabInbox,
            MatterSetupStage.SuccessInboxDetected,
            MatterSetupStage.Failed -> null
        }
    }
}
