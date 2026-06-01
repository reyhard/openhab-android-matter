package org.openhab.matter.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenHabInboxBrowserTargetTest {
    @Test
    fun appendsInboxPathToConfiguredOpenHabAddress() {
        assertEquals(
            "http://openhab.local:8080/settings/things/inbox",
            OpenHabInboxBrowserTarget.url(" http://openhab.local:8080 ")
        )
    }

    @Test
    fun removesTrailingSlashesBeforeAppendingInboxPath() {
        assertEquals(
            "https://openhab.local:8443/settings/things/inbox",
            OpenHabInboxBrowserTarget.url("https://openhab.local:8443///")
        )
    }
}
