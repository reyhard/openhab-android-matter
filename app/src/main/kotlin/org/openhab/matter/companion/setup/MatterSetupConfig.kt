package org.openhab.matter.companion.setup

data class MatterSetupConfig(
    val openHabBaseUrl: String,
    val openHabApiToken: String,
    val threadDataset: String,
    val otbrBaseUrl: String,
    val attestationBypassEnabled: Boolean
) {
    val openHabConfigured: Boolean
        get() = openHabBaseUrl.isNotBlank()
}
