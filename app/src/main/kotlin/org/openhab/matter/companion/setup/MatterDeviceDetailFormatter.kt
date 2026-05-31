package org.openhab.matter.companion.setup

object MatterDeviceDetailFormatter {
    const val UNKNOWN = "Unknown"
    const val UNKNOWN_PRODUCT = "Unknown Matter device"
    const val UNKNOWN_VENDOR = "Unknown vendor"

    fun nodeId(nodeId: Long?): String {
        return nodeId?.let { "0x${java.lang.Long.toUnsignedString(it, 16).uppercase()}" } ?: UNKNOWN
    }

    fun display(value: String, fallback: String = UNKNOWN): String {
        return value.trim().ifBlank { fallback }
    }

    fun battery(halfPercent: Int?, quantity: Int?, designation: String): String {
        if (halfPercent == null) {
            return UNKNOWN
        }
        val percent = if (halfPercent % 2 == 0) {
            "${halfPercent / 2}%"
        } else {
            "${halfPercent / 2}.5%"
        }
        val type = designation.trim()
        return if (quantity != null && quantity > 0 && type.isNotBlank()) {
            "$percent · ${quantity}×$type"
        } else {
            percent
        }
    }

    fun threadNetwork(name: String, channel: Int?): String {
        val parts = listOfNotNull(
            name.trim().takeIf { it.isNotBlank() },
            channel?.let { "Channel $it" }
        )
        return parts.joinToString(" · ").ifBlank { UNKNOWN }
    }

    fun otaUpdate(updatePossible: Boolean?): String {
        return when (updatePossible) {
            true -> "Possible"
            false -> "Not available"
            null -> UNKNOWN
        }
    }
}
