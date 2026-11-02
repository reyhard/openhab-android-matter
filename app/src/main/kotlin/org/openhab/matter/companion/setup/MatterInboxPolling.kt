package org.openhab.matter.companion.setup

import org.openhab.matter.companion.openhab.OpenHabInboxEntry
import org.openhab.matter.companion.openhab.OpenHabInboxStatus

internal data class MatterInboxSelection(
    val entry: OpenHabInboxEntry,
    val labelConfirmed: Boolean,
    val nodeIdMatched: Boolean
)

internal fun OpenHabInboxStatus.selectMatterInboxEntry(
    discoveryContext: MatterDiscoveryContext
): MatterInboxSelection? {
    val eligibleEntries = entries().filter { it.eligibleDiscovery() }
    if (eligibleEntries.isEmpty()) {
        return null
    }

    val vendorName = discoveryContext.vendorName.trim()
    val productName = discoveryContext.productName.trim()
    if (vendorName.isNotEmpty() && productName.isNotEmpty()) {
        eligibleEntries.firstOrNull { it.labelContains(vendorName, productName) }?.let { entry ->
            return MatterInboxSelection(entry, labelConfirmed = true, nodeIdMatched = false)
        }
    }

    discoveryContext.nodeId?.let { nodeId ->
        eligibleEntries.firstOrNull { it.matchesNodeId(nodeId) }?.let { entry ->
            return MatterInboxSelection(entry, labelConfirmed = false, nodeIdMatched = true)
        }
    }

    return MatterInboxSelection(eligibleEntries.first(), labelConfirmed = false, nodeIdMatched = false)
}

internal fun pollForMatterInbox(
    timeoutSeconds: Int,
    discoveryContext: MatterDiscoveryContext,
    checkInbox: () -> OpenHabInboxStatus,
    clock: () -> Long = System::currentTimeMillis,
    sleep: (Long) -> Unit = { Thread.sleep(it) }
): MatterSetupPorts.InboxResult {
    val deadlineMillis = clock() + (timeoutSeconds.coerceAtLeast(30) * 1000L)
    var last = OpenHabInboxStatus(false, false, "openHAB Inbox was not checked", "")
    var selection: MatterInboxSelection? = null
    var pollCount = 0

    do {
        pollCount++
        last = runCatching { checkInbox() }.getOrElse { error ->
            OpenHabInboxStatus(
                false,
                false,
                "openHAB Inbox check failed",
                error.message.orEmpty()
            )
        }
        selection = last.selectMatterInboxEntry(discoveryContext)
        if (selection != null) {
            break
        }
        if (last.responseIncomplete()) {
            break
        }

        val remainingMillis = deadlineMillis - clock()
        if (remainingMillis <= 0L) {
            break
        }
        try {
            sleep(remainingMillis.coerceAtMost(2_000L))
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            break
        }
    } while (true)

    val details = buildList {
        add(last.message())
        add(last.details())
        if (selection == null) {
            if (last.responseIncomplete()) {
                add("No Inbox entry was accepted from the incomplete response.")
            } else if (last.matterEntryDetected()) {
                add("No non-ignored Matter Inbox entry was detected.")
            }
        }
    }
        .filter { it.isNotBlank() }
        .joinToString(": ")

    return MatterSetupPorts.InboxResult(
        matterEntryDetected = selection != null,
        details = details,
        matterEntryIds = last.matterEntryIds(),
        labelConfirmed = selection?.labelConfirmed == true,
        detectedOnFirstPoll = selection != null && pollCount == 1,
        reachable = last.reachable(),
        responseIncomplete = last.responseIncomplete()
    )
}

internal fun MatterSetupPorts.InboxResult.failureMessage(): String {
    return when {
        !reachable -> "openHAB Inbox could not be checked"
        responseIncomplete -> "openHAB Inbox response was incomplete"
        else -> "openHAB did not report the device yet"
    }
}

internal fun MatterSetupPorts.InboxResult.successMessage(): String {
    if (!labelConfirmed) {
        return "A fresh Matter Inbox entry was found, but its identity could not be confirmed."
    }
    if (detectedOnFirstPoll) {
        return "openHAB already knows this device."
    }
    return "openHAB reported a Matter Inbox entry for this device."
}
