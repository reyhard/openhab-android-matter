package org.openhab.matter.companion.setup

enum class MatterSetupStage {
    NeedsOpenHabSetup,
    OpenHabSetupChecking,
    ReadyToScan,
    ScanningQr,
    QrScanned,
    ReadinessChecking,
    NeedsPairingMode,
    CommissioningToPhone,
    OpeningCommissioningWindow,
    CommissioningWindowOpen,
    SendingCodeToOpenHab,
    WatchingOpenHabInbox,
    SuccessInboxDetected,
    Failed,
    PhoneDeviceList,
    AdvancedTroubleshooting
}
