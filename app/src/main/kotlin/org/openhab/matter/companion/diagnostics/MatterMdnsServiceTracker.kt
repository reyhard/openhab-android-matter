package org.openhab.matter.companion.diagnostics

/**
 * Tracks the current DNS-SD discovery generation for an instance.
 * A late resolve callback must not resurrect a service that was lost (or re-advertised).
 * All accesses are serialized by the caller's discovery lock.
 */
internal class MatterMdnsServiceTracker {
    data class Ticket(val key: String, val generation: Long)

    private val active = mutableMapOf<String, Long>()
    private val records = linkedMapOf<String, MatterMdnsRecord>()
    private var nextGeneration = 0L

    fun found(key: String): Ticket? {
        if (active.containsKey(key)) return null
        val ticket = Ticket(key, ++nextGeneration)
        active[key] = ticket.generation
        return ticket
    }

    fun lost(key: String) {
        active.remove(key)
        records.remove(key)
    }

    fun resolved(ticket: Ticket, record: MatterMdnsRecord?) {
        if (record != null && active[ticket.key] == ticket.generation) {
            records[ticket.key] = record
        }
    }

    fun snapshot(): List<MatterMdnsRecord> = records.values.toList()
}
