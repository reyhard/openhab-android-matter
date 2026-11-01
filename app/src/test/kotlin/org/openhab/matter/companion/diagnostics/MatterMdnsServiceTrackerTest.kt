package org.openhab.matter.companion.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatterMdnsServiceTrackerTest {
    private fun record(name: String) = MatterMdnsRecord(
        serviceType = "_matterc._udp",
        instanceName = name,
        host = "device.local",
        ipv6Addresses = listOf("fd00::1"),
        port = 5540,
        txt = mapOf("D" to "2600")
    )

    @Test
    fun lostServiceIsRemovedFromCurrentResults() {
        val tracker = MatterMdnsServiceTracker()
        val ticket = tracker.found("service")!!
        tracker.resolved(ticket, record("old"))
        assertEquals(1, tracker.snapshot().size)

        tracker.lost("service")

        assertTrue(tracker.snapshot().isEmpty())
    }

    @Test
    fun lateResolveCannotResurrectLostOrReadvertisedService() {
        val tracker = MatterMdnsServiceTracker()
        val oldTicket = tracker.found("service")!!
        tracker.lost("service")
        val newTicket = tracker.found("service")!!
        tracker.resolved(oldTicket, record("old"))
        assertTrue(tracker.snapshot().isEmpty())

        tracker.resolved(newTicket, record("new"))
        assertEquals("new", tracker.snapshot().single().instanceName)
    }

    @Test
    fun duplicateFoundEventDoesNotScheduleAnotherResolveUntilServiceIsLost() {
        val tracker = MatterMdnsServiceTracker()
        assertNotNull(tracker.found("service"))
        assertNull(tracker.found("service"))
        tracker.lost("service")
        assertNotNull(tracker.found("service"))
    }
}
