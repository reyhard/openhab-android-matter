package org.openhab.matter.companion.diagnostics

interface MatterMdnsBrowser {
    fun browseMatterCommissionable(timeoutMillis: Long = 5_000L): List<MatterMdnsRecord>

    fun browseMatterOperational(timeoutMillis: Long = 5_000L): List<MatterMdnsRecord>
}
