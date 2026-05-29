package org.openhab.matter.companion.setup

data class MatterSetupDeviceIdentity(
    val vendorName: String = "",
    val productName: String = ""
) {
    val displayName: String
        get() = listOf(vendorName, productName)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(" ")

    fun visible(): Boolean {
        return displayName.isNotBlank()
    }
}
