package org.openhab.matter.companion.ui

object OpenHabInboxBrowserTarget {
    fun url(openHabBaseUrl: String): String {
        return openHabBaseUrl.trim().trimEnd('/') + "/settings/things/inbox"
    }
}
