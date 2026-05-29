package org.openhab.matter.companion.setup

import java.net.URI

private val HttpUrlTokenRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
private val UrlTrailingPunctuation = setOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '"', '\'')

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

internal fun String.sanitizeLogUrls(): String {
    return HttpUrlTokenRegex.replace(this) { match ->
        val token = match.value
        val urlEnd = token.indexOfLast { it !in UrlTrailingPunctuation } + 1
        val url = token.take(urlEnd)
        val trailing = token.drop(urlEnd)
        url.toLogSafeUrl() + trailing
    }
}
