package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openhab.matter.companion.openhab.OpenHabInboxEntry
import org.openhab.matter.companion.openhab.OpenHabInboxStatus

class MatterInboxPollingTest {
    private val openHabNodeId = java.lang.Long.parseUnsignedLong("10051033818082827676")
    private val phoneNodeId = java.lang.Long.parseUnsignedLong("3FE921A7A8C9DA6C", 16)

    @Test
    fun onlyExplicitNonIgnoredMatterEntriesQualify() {
        val ignored = entry("matter:node:OTBR_01:1", flag = "IGNORED", nodeId = "1")
        val missingFlag = entry("matter:node:OTBR_01:2", flag = "", nodeId = "2")
        val nonMatter = entry("zwave:device:3", flag = "NEW")

        assertNull(status(ignored, missingFlag, nonMatter).selectMatterInboxEntry(context()))
    }

    @Test
    fun acceptsMatterEntryWhenFlagIsNewAndIgnoresNonMatterNewEntry() {
        val nonMatter = entry("zwave:device:3", flag = "NEW")
        val matter = entry("matter:node:OTBR_01:3", flag = "NEW", nodeId = "3")

        val selection = status(nonMatter, matter).selectMatterInboxEntry(context())

        assertNotNull(selection)
        assertEquals(matter.thingUid(), selection?.entry?.thingUid())
        assertFalse(selection?.labelConfirmed == true)
    }

    @Test
    fun prefersLabelConfirmedEntryWhenVendorAndProductAreAvailable() {
        val unrelated = entry("matter:node:OTBR_01:10", label = "Unrelated Device", flag = "NEW", nodeId = "10")
        val target = entry(
            "matter:node:OTBR_01:10051033818082827676",
            label = "Matter Device: IKEA of Sweden ALPSTUGA air quality monitor",
            flag = "NEW",
            nodeId = "10051033818082827676"
        )

        val selection = status(unrelated, target).selectMatterInboxEntry(
            context(vendorName = "IKEA of Sweden", productName = "ALPSTUGA")
        )

        assertEquals(target.thingUid(), selection?.entry?.thingUid())
        assertTrue(selection?.labelConfirmed == true)
    }

    @Test
    fun acceptsUnrelatedNewMatterEntryAsUnconfirmedWhenLabelDoesNotMatch() {
        val unrelated = entry("matter:node:OTBR_01:11", label = "Some Other Vendor Other Product", flag = "NEW")

        val selection = status(unrelated).selectMatterInboxEntry(
            context(vendorName = "IKEA of Sweden", productName = "ALPSTUGA")
        )

        assertEquals(unrelated.thingUid(), selection?.entry?.thingUid())
        assertFalse(selection?.labelConfirmed == true)
    }

    @Test
    fun nodeIdMatchIsCorroborationOnlyForEligibleEntries() {
        val ignored = entry(
            "matter:node:OTBR_01:10051033818082827676",
            flag = "IGNORED",
            nodeId = "10051033818082827676"
        )
        val missingFlag = entry(
            "matter:node:OTBR_01:10051033818082827676",
            flag = "",
            nodeId = "10051033818082827676"
        )

        assertNull(status(ignored, missingFlag).selectMatterInboxEntry(context(nodeId = openHabNodeId)))
    }

    @Test
    fun nodeIdMatchPrefersEligibleEntryWithoutBecomingTheSoleSuccessSignal() {
        val unrelated = entry("matter:node:OTBR_01:12", label = "Unrelated", flag = "NEW", nodeId = "12")
        val nodeMatch = entry(
            "matter:node:OTBR_01:10051033818082827676",
            label = "Unrelated label",
            flag = "NEW",
            nodeId = "10051033818082827676"
        )

        val selection = status(unrelated, nodeMatch).selectMatterInboxEntry(
            context(nodeId = openHabNodeId)
        )

        assertEquals(nodeMatch.thingUid(), selection?.entry?.thingUid())
        assertTrue(selection?.nodeIdMatched == true)
        assertFalse(selection?.labelConfirmed == true)
    }

    @Test
    fun mismatchedPhoneAndOpenHabNodeIdsDoNotPreventEligibleDiscovery() {
        val target = entry(
            "matter:node:OTBR_01:10051033818082827676",
            label = "IKEA ALPSTUGA",
            flag = "NEW",
            nodeId = "10051033818082827676"
        )

        val selection = status(target).selectMatterInboxEntry(
            context(vendorName = "IKEA", productName = "ALPSTUGA", nodeId = phoneNodeId)
        )

        assertEquals(target.thingUid(), selection?.entry?.thingUid())
        assertTrue(selection?.labelConfirmed == true)
    }

    @Test
    fun pollingAcceptsALaterNewEntryAfterIgnoredResponses() {
        var now = 0L
        val responses = ArrayDeque(
            listOf(
                status(entry("matter:node:OTBR_01:1", flag = "IGNORED", nodeId = "1")),
                status(entry("matter:node:OTBR_01:2", flag = "", nodeId = "2")),
                status(entry("matter:node:OTBR_01:3", flag = "NEW", nodeId = "3"))
            )
        )

        val result = pollForMatterInbox(
            timeoutSeconds = 30,
            discoveryContext = context(),
            checkInbox = { responses.removeFirst() },
            clock = { now },
            sleep = { delay -> now += delay }
        )

        assertTrue(result.matterEntryDetected)
        assertEquals(setOf("matter:node:OTBR_01:3"), result.matterEntryIds)
        assertFalse(result.detectedOnFirstPoll)
    }

    @Test
    fun pollingReportsUnreachableInboxAfterTimeout() {
        var now = 0L
        val unreachable = OpenHabInboxStatus(
            false,
            false,
            "openHAB Inbox is not reachable",
            "Connection refused",
            emptyList()
        )

        val result = pollForMatterInbox(
            timeoutSeconds = 30,
            discoveryContext = context(),
            checkInbox = { unreachable },
            clock = { now },
            sleep = { delay -> now += delay }
        )

        assertFalse(result.matterEntryDetected)
        assertFalse(result.reachable)
        assertTrue(result.details.contains("not reachable"))
    }

    @Test
    fun pollingReportsIncompleteResponseDistinctly() {
        var checks = 0
        val incomplete = OpenHabInboxStatus(
            true,
            false,
            "openHAB Inbox response exceeded the 8388608-byte limit",
            "HTTP 200; response exceeded the 8388608-byte limit and was not checked",
            emptyList(),
            true
        )

        val result = pollForMatterInbox(
            timeoutSeconds = 30,
            discoveryContext = context(),
            checkInbox = {
                checks++
                incomplete
            },
            clock = { 0L },
            sleep = { }
        )

        assertEquals(1, checks)
        assertFalse(result.matterEntryDetected)
        assertTrue(result.responseIncomplete)
        assertEquals("openHAB Inbox response was incomplete", result.failureMessage())
        assertTrue(result.details.contains("8388608"))
    }

    @Test
    fun pollingReportsFirstPollLabelConfirmationAsAlreadyKnown() {
        val target = entry(
            "matter:node:OTBR_01:10051033818082827676",
            label = "IKEA ALPSTUGA air quality monitor",
            flag = "NEW",
            nodeId = "10051033818082827676"
        )

        val result = pollForMatterInbox(
            timeoutSeconds = 30,
            discoveryContext = context(vendorName = "IKEA", productName = "ALPSTUGA"),
            checkInbox = { status(target) },
            clock = { 0L },
            sleep = { }
        )

        assertTrue(result.matterEntryDetected)
        assertTrue(result.labelConfirmed)
        assertTrue(result.detectedOnFirstPoll)
        assertEquals("openHAB already knows this device.", result.successMessage())
    }

    private fun context(
        vendorName: String = "",
        productName: String = "",
        nodeId: Long? = null
    ): MatterDiscoveryContext {
        return MatterDiscoveryContext(vendorName, productName, nodeId)
    }

    private fun entry(
        thingUid: String,
        label: String = "Matter Device",
        flag: String = "NEW",
        nodeId: String = ""
    ): OpenHabInboxEntry {
        return OpenHabInboxEntry(thingUid, label, flag, nodeId)
    }

    private fun status(vararg entries: OpenHabInboxEntry): OpenHabInboxStatus {
        val matterDetected = entries.any { it.matterEntry() }
        return OpenHabInboxStatus(
            true,
            matterDetected,
            if (matterDetected) "Matter Inbox entry detected" else "No Matter Inbox entry detected",
            "HTTP 200",
            entries.toList()
        )
    }
}
