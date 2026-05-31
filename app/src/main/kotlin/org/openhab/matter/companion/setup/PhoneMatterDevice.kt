package org.openhab.matter.companion.setup

import org.openhab.matter.companion.controller.MatterBootstrapState
import org.openhab.matter.companion.controller.MatterDeviceDetails

data class PhoneMatterDevice(
    val nodeId: Long?,
    val controllerStateStored: Boolean,
    val stateReadable: Boolean,
    val vendorName: String = "",
    val productName: String = "",
    val fetchedDetails: MatterDeviceDetails = MatterDeviceDetails.empty()
) {
    val displayName: String
        get() = listOf(vendorName, productName)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifBlank { displayNodeId }

    val displayProductName: String
        get() = MatterDeviceDetailFormatter.display(
            productName,
            MatterDeviceDetailFormatter.UNKNOWN_PRODUCT
        )

    val displayVendorName: String
        get() = MatterDeviceDetailFormatter.display(
            vendorName,
            MatterDeviceDetailFormatter.UNKNOWN_VENDOR
        )

    val displayNodeId: String
        get() = MatterDeviceDetailFormatter.nodeId(nodeId)

    val displayControllerState: String
        get() = if (controllerStateStored) {
            "stored"
        } else {
            "missing"
        }

    val displayStateReadable: String
        get() = if (stateReadable) {
            "yes"
        } else {
            "no"
        }

    val status: String
        get() = when {
            !stateReadable -> "Stored state is unreadable"
            controllerStateStored -> "Ready to open a pairing window for openHAB"
            else -> "Controller state is missing"
        }

    val canOpenCommissioningWindow: Boolean
        get() = nodeId != null && controllerStateStored && stateReadable

    val canAttemptCommissioningWindowForDebug: Boolean
        get() = nodeId != null && stateReadable

    fun initialDetails(): PhoneMatterDeviceDetails {
        val baseDetails = PhoneMatterDeviceDetails(
            deviceName = displayProductName,
            vendor = displayVendorName,
            product = MatterDeviceDetailFormatter.display(productName),
            nodeId = displayNodeId
        )
        return if (fetchedDetails.isEmpty) {
            baseDetails
        } else {
            baseDetails.merge(phoneMatterDeviceDetailsFromControllerDetails(fetchedDetails, this))
        }
    }

    companion object {
        fun fromBootstrapState(state: MatterBootstrapState): PhoneMatterDevice? {
            val hasNode = state.bootstrapNodeId() >= 0L
            val hasControllerState = state.controllerState().isNotBlank()
            if (!hasNode && !hasControllerState && !state.stateUnreadable()) {
                return null
            }
            return PhoneMatterDevice(
                nodeId = state.bootstrapNodeId().takeIf { it >= 0L },
                controllerStateStored = hasControllerState,
                stateReadable = !state.stateUnreadable(),
                vendorName = state.vendorName(),
                productName = state.productName(),
                fetchedDetails = state.deviceDetails()
            )
        }
    }
}

internal fun phoneMatterDeviceDetailsFromControllerDetails(
    details: MatterDeviceDetails,
    device: PhoneMatterDevice
): PhoneMatterDeviceDetails {
    val product = details.productName().ifBlank { device.productName.trim() }
    val vendor = details.vendorName().ifBlank { device.vendorName.trim() }
    return PhoneMatterDeviceDetails(
        deviceName = product.takeIf { it.isNotBlank() }?.let {
            MatterDeviceDetailFormatter.display(it, MatterDeviceDetailFormatter.UNKNOWN_PRODUCT)
        }.orEmpty(),
        vendor = vendor.takeIf { it.isNotBlank() }?.let {
            MatterDeviceDetailFormatter.display(it, MatterDeviceDetailFormatter.UNKNOWN_VENDOR)
        }.orEmpty(),
        product = product.takeIf { it.isNotBlank() }?.let(MatterDeviceDetailFormatter::display).orEmpty(),
        firmwareVersion = details.softwareVersionString(),
        hardwareVersion = details.hardwareVersionString(),
        partNumber = details.partNumber(),
        nodeId = device.displayNodeId,
        battery = if (details.batteryPercentRemaining() != null) {
            MatterDeviceDetailFormatter.battery(
                details.batteryPercentRemaining(),
                details.batteryQuantity(),
                details.batteryDesignation()
            )
        } else {
            ""
        },
        threadNetwork = if (details.threadNetworkName().isNotBlank() || details.threadChannel() != null) {
            MatterDeviceDetailFormatter.threadNetwork(
                details.threadNetworkName(),
                details.threadChannel()
            )
        } else {
            ""
        },
        ipv6Address = details.ipv6Address(),
        otaUpdate = details.otaUpdatePossible()?.let(MatterDeviceDetailFormatter::otaUpdate).orEmpty()
    )
}
