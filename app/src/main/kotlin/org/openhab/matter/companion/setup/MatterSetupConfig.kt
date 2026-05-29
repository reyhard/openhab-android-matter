package org.openhab.matter.companion.setup

import java.net.URI

data class MatterSetupConfig(
    val openHabBaseUrl: String,
    val openHabApiToken: String,
    val threadDataset: String,
    val otbrBaseUrl: String,
    val attestationBypassEnabled: Boolean
) {
    val openHabConfigured: Boolean
        get() = openHabBaseUrl.isNotBlank()

    override fun toString(): String {
        return "MatterSetupConfig(" +
            "openHabBaseUrl=${openHabBaseUrl.toLogSafeUrl()}, " +
            "openHabApiToken=<redacted>, " +
            "threadDataset=<redacted>, " +
            "otbrBaseUrl=${otbrBaseUrl.toLogSafeUrl()}, " +
            "attestationBypassEnabled=$attestationBypassEnabled" +
            ")"
    }
}

internal fun String.toLogSafeUrl(): String {
    if (isBlank()) {
        return ""
    }
    return runCatching {
        val uri = URI(this)
        val scheme = uri.scheme ?: return@runCatching "<redacted>"
        val host = uri.host ?: return@runCatching "<redacted>"
        val port = if (uri.port >= 0) ":${uri.port}" else ""
        val path = uri.rawPath.orEmpty()
        "$scheme://$host$port$path"
    }.getOrElse {
        "<redacted>"
    }
}
