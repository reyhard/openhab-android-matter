package org.openhab.matter.companion.ui

data class ScanReadinessUiState(
    val bluetoothReady: Boolean,
    val locationReady: Boolean,
    val permissionsReady: Boolean,
    val bluetoothMessage: String = "",
    val locationMessage: String = "",
    val permissionsMessage: String = "",
    val bluetoothHelperAvailable: Boolean = false,
    val locationHelperAvailable: Boolean = false,
    val permissionsHelperAvailable: Boolean = false
) {
    val ready: Boolean
        get() = bluetoothReady && locationReady && permissionsReady

    companion object {
        fun ready(): ScanReadinessUiState {
            return ScanReadinessUiState(
                bluetoothReady = true,
                locationReady = true,
                permissionsReady = true,
                bluetoothMessage = "Bluetooth is enabled.",
                locationMessage = "Location services are enabled.",
                permissionsMessage = "Required permissions are granted."
            )
        }
    }
}
