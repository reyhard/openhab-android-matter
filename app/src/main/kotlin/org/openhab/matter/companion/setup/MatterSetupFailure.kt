package org.openhab.matter.companion.setup

data class MatterSetupFailure(
    val step: MatterSetupStage,
    val message: String,
    val details: String = "",
    val suggestions: List<String> = defaultSuggestions(step)
) {
    companion object {
        fun defaultSuggestions(step: MatterSetupStage): List<String> = when (step) {
            MatterSetupStage.ScanningQr,
            MatterSetupStage.QrScanned -> listOf("Scan the Matter QR code again.", "Enter the setup code manually.")

            MatterSetupStage.CommissioningToPhone -> listOf(
                "Keep the phone near the device.",
                "Confirm the device is still in pairing mode.",
                "Check Thread network and IPv6 routing if BLE already connected."
            )

            MatterSetupStage.OpeningCommissioningWindow,
            MatterSetupStage.CommissioningWindowOpen -> listOf(
                "Open the pairing window again.",
                "Run setup again if this phone no longer has the device staged."
            )

            MatterSetupStage.SendingCodeToOpenHab -> listOf(
                "Check the openHAB address and access token.",
                "Check that the openHAB Matter controller Thing is online."
            )

            MatterSetupStage.WatchingOpenHabInbox -> listOf(
                "Check IPv6 routing between openHAB and the Thread network.",
                "Check mDNS or Avahi on the openHAB side.",
                "Open the pairing window again if the countdown expired."
            )

            else -> listOf("Try again.", "Open troubleshooting for more checks.")
        }
    }
}
