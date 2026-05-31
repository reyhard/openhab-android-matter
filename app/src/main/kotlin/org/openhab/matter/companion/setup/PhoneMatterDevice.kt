package org.openhab.matter.companion.setup

import org.openhab.matter.companion.controller.MatterBootstrapState

data class PhoneMatterDevice(
    val nodeId: Long?,
    val controllerStateStored: Boolean,
    val stateReadable: Boolean,
    val vendorName: String = "",
    val productName: String = ""
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
        return PhoneMatterDeviceDetails(
            deviceName = displayProductName,
            vendor = displayVendorName,
            product = MatterDeviceDetailFormatter.display(productName),
            nodeId = displayNodeId
        )
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
                productName = state.productName()
            )
        }
    }
}
