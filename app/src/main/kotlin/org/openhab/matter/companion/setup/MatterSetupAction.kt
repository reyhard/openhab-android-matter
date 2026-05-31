package org.openhab.matter.companion.setup

sealed interface MatterSetupAction {
    data object GetStarted : MatterSetupAction
    data object SaveOpenHab : MatterSetupAction
    data object TestOpenHab : MatterSetupAction
    data object TestSettings : MatterSetupAction
    data object EditSettings : MatterSetupAction
    data object EditOpenHabAddress : MatterSetupAction
    data object SaveOpenHabAddress : MatterSetupAction
    data object ChangeToken : MatterSetupAction
    data object SaveChangedToken : MatterSetupAction
    data object EditThreadNetwork : MatterSetupAction
    data object CheckThreadDataset : MatterSetupAction
    data object SaveThreadSettings : MatterSetupAction
    data object DetectThreadBorderRouters : MatterSetupAction
    data class SelectThreadBorderRouter(val endpoint: String) : MatterSetupAction
    data object BackToMainMenu : MatterSetupAction
    data object BackToSettings : MatterSetupAction
    data object BackToRequiredSetup : MatterSetupAction
    data object ShowPhoneDevices : MatterSetupAction
    data class ShowPhoneDeviceDetails(val nodeId: Long?) : MatterSetupAction
    data object FetchPhoneDeviceDetails : MatterSetupAction
    data object StartScan : MatterSetupAction
    data object OpenBluetoothSettings : MatterSetupAction
    data object OpenLocationSettings : MatterSetupAction
    data object RequestSetupPermissions : MatterSetupAction
    data object EnterCodeManually : MatterSetupAction
    data object SubmitManualCode : MatterSetupAction
    data object ConfirmPairingMode : MatterSetupAction
    data object Retry : MatterSetupAction
    data object OpenCommissioningWindowAgain : MatterSetupAction
    data object BrowseMatterServices : MatterSetupAction
    data object CheckIpv6Reachability : MatterSetupAction
    data object ShowTroubleshooting : MatterSetupAction
    data object ForgetFromPhone : MatterSetupAction
    data object AddAnotherDevice : MatterSetupAction
}
