package org.openhab.matter.companion.setup

enum class MatterSetupStage {
    Welcome,
    NeedsOpenHabSetup,
    OpenHabSetupChecking,
    ReadyToScan,
    ScanningQr,
    QrScanned,
    EnteringManualCode,
    ReadinessChecking,
    NeedsPairingMode,
    CommissioningToPhone,
    OpeningCommissioningWindow,
    CommissioningWindowOpen,
    SendingCodeToOpenHab,
    WatchingOpenHabInbox,
    SuccessInboxDetected,
    Failed,
    Settings,
    OpenHabAddressEditor,
    ChangeToken,
    ThreadNetworkEditor,
    PhoneDeviceList,
    PhoneDeviceDetails,
    AdvancedTroubleshooting
}
