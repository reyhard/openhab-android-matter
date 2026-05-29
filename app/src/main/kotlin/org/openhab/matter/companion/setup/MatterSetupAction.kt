package org.openhab.matter.companion.setup

sealed interface MatterSetupAction {
    data object SaveOpenHab : MatterSetupAction
    data object TestOpenHab : MatterSetupAction
    data object EditSettings : MatterSetupAction
    data object CheckThreadDataset : MatterSetupAction
    data object SaveThreadSettings : MatterSetupAction
    data object DetectThreadBorderRouters : MatterSetupAction
    data class SelectThreadBorderRouter(val endpoint: String) : MatterSetupAction
    data object BackToMainMenu : MatterSetupAction
    data object StartScan : MatterSetupAction
    data object EnterCodeManually : MatterSetupAction
    data object ConfirmPairingMode : MatterSetupAction
    data object Retry : MatterSetupAction
    data object OpenCommissioningWindowAgain : MatterSetupAction
    data object ShowTroubleshooting : MatterSetupAction
    data object ForgetFromPhone : MatterSetupAction
    data object AddAnotherDevice : MatterSetupAction
}
