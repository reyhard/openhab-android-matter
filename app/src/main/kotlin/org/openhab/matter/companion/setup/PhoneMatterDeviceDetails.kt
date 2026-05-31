package org.openhab.matter.companion.setup

data class PhoneMatterDeviceDetails(
    val deviceName: String = "",
    val vendor: String = "",
    val product: String = "",
    val firmwareVersion: String = "",
    val hardwareVersion: String = "",
    val partNumber: String = "",
    val nodeId: String = "",
    val battery: String = "",
    val threadNetwork: String = "",
    val ipv6Address: String = "",
    val otaUpdate: String = ""
) {
    fun merge(update: PhoneMatterDeviceDetails): PhoneMatterDeviceDetails {
        return copy(
            deviceName = update.deviceName.ifBlank { deviceName },
            vendor = update.vendor.ifBlank { vendor },
            product = update.product.ifBlank { product },
            firmwareVersion = update.firmwareVersion.ifBlank { firmwareVersion },
            hardwareVersion = update.hardwareVersion.ifBlank { hardwareVersion },
            partNumber = update.partNumber.ifBlank { partNumber },
            nodeId = update.nodeId.ifBlank { nodeId },
            battery = update.battery.ifBlank { battery },
            threadNetwork = update.threadNetwork.ifBlank { threadNetwork },
            ipv6Address = update.ipv6Address.ifBlank { ipv6Address },
            otaUpdate = update.otaUpdate.ifBlank { otaUpdate }
        )
    }
}
