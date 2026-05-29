package org.openhab.matter.companion.diagnostics

interface MatterMdnsBrowser {
    /**
     * Synchronously browses for Matter commissionable mDNS records. Call from a worker thread.
     */
    fun browseMatterCommissionable(timeoutMillis: Long = 5_000L): List<MatterMdnsRecord>

    /**
     * Synchronously browses for Matter operational mDNS records. Call from a worker thread.
     */
    fun browseMatterOperational(timeoutMillis: Long = 5_000L): List<MatterMdnsRecord>
}
