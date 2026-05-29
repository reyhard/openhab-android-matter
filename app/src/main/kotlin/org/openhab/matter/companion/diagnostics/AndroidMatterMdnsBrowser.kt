package org.openhab.matter.companion.diagnostics

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class AndroidMatterMdnsBrowser(
    context: Context
) : MatterMdnsBrowser {
    private val nsdManager: NsdManager? = context.getSystemService(NsdManager::class.java)

    override fun browseMatterCommissionable(timeoutMillis: Long): List<MatterMdnsRecord> {
        return browse(serviceType = MATTER_COMMISSIONABLE_SERVICE_TYPE, timeoutMillis = timeoutMillis)
    }

    override fun browseMatterOperational(timeoutMillis: Long): List<MatterMdnsRecord> {
        return browse(serviceType = MATTER_OPERATIONAL_SERVICE_TYPE, timeoutMillis = timeoutMillis)
    }

    private fun browse(serviceType: String, timeoutMillis: Long): List<MatterMdnsRecord> {
        val manager = nsdManager ?: return emptyList()
        val records = Collections.synchronizedList(mutableListOf<MatterMdnsRecord>())
        val seenServices = Collections.synchronizedSet(mutableSetOf<String>())
        val discoveryFinished = CountDownLatch(1)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val key = "${serviceInfo.serviceType}/${serviceInfo.serviceName}"
                if (!seenServices.add(key)) {
                    return
                }
                resolve(manager, serviceType, serviceInfo, records)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

            override fun onDiscoveryStopped(serviceType: String) {
                discoveryFinished.countDown()
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                discoveryFinished.countDown()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                discoveryFinished.countDown()
            }
        }

        return try {
            manager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            discoveryFinished.await(timeoutMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            stopDiscovery(manager, listener)
            records.toList()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            emptyList()
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: IllegalArgumentException) {
            emptyList()
        } catch (e: RuntimeException) {
            emptyList()
        }
    }

    private fun resolve(
        manager: NsdManager,
        requestedServiceType: String,
        serviceInfo: NsdServiceInfo,
        records: MutableList<MatterMdnsRecord>
    ) {
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                records.add(serviceInfo.toMatterMdnsRecord(requestedServiceType))
            }
        }

        try {
            manager.resolveService(serviceInfo, listener)
        } catch (e: SecurityException) {
            return
        } catch (e: IllegalArgumentException) {
            return
        } catch (e: RuntimeException) {
            return
        }
    }

    private fun stopDiscovery(manager: NsdManager, listener: NsdManager.DiscoveryListener) {
        try {
            manager.stopServiceDiscovery(listener)
        } catch (e: IllegalArgumentException) {
            return
        } catch (e: RuntimeException) {
            return
        }
    }

    private fun NsdServiceInfo.toMatterMdnsRecord(requestedServiceType: String): MatterMdnsRecord {
        val hostAddress = host?.hostAddress.orEmpty()
        val ipv6Addresses = if (DefaultIpv6ReachabilityProbe.isIpv6Literal(hostAddress)) {
            listOf(hostAddress)
        } else {
            emptyList()
        }

        return MatterMdnsRecord(
            serviceType = serviceType.ifBlank { requestedServiceType },
            instanceName = serviceName,
            host = host?.hostName.orEmpty(),
            ipv6Addresses = ipv6Addresses,
            port = port,
            txt = attributes.mapValues { (_, value) ->
                value.toString(StandardCharsets.UTF_8)
            }
        )
    }

    private companion object {
        const val MATTER_COMMISSIONABLE_SERVICE_TYPE = "_matterc._udp"
        const val MATTER_OPERATIONAL_SERVICE_TYPE = "_matter._tcp"
    }
}
