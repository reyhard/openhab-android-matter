package org.openhab.matter.companion.setup

class MatterSetupStage private constructor(private val label: String) {
    override fun toString(): String = label

    companion object {
        val Welcome = MatterSetupStage("Welcome")
        val NeedsOpenHabSetup = MatterSetupStage("NeedsOpenHabSetup")
        val OpenHabSetupChecking = MatterSetupStage("OpenHabSetupChecking")
        val ReadyToScan = MatterSetupStage("ReadyToScan")
        val ScanningQr = MatterSetupStage("ScanningQr")
        val QrScanned = MatterSetupStage("QrScanned")
        val EnteringManualCode = MatterSetupStage("EnteringManualCode")
        val ReadinessChecking = MatterSetupStage("ReadinessChecking")
        val NeedsPairingMode = MatterSetupStage("NeedsPairingMode")
        val CommissioningToPhone = MatterSetupStage("CommissioningToPhone")
        val OpeningCommissioningWindow = MatterSetupStage("OpeningCommissioningWindow")
        val CommissioningWindowOpen = MatterSetupStage("CommissioningWindowOpen")
        val SendingCodeToOpenHab = MatterSetupStage("SendingCodeToOpenHab")
        val WatchingOpenHabInbox = MatterSetupStage("WatchingOpenHabInbox")
        val SuccessInboxDetected = MatterSetupStage("SuccessInboxDetected")
        val Failed = MatterSetupStage("Failed")
        val Settings = MatterSetupStage("Settings")
        val ChangeToken = MatterSetupStage("ChangeToken")
        val ThreadNetworkEditor = MatterSetupStage("ThreadNetworkEditor")
        val PhoneDeviceList = MatterSetupStage("PhoneDeviceList")
        val AdvancedTroubleshooting = MatterSetupStage("AdvancedTroubleshooting")
    }
}
