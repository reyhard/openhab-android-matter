package org.openhab.matter.companion.diagnostics

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.nio.charset.StandardCharsets
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
        val stateLock = Object()
        val tracker = MatterMdnsServiceTracker()
        val pendingServices = ArrayDeque<PendingResolution>()
        var resolveInProgress = false
        var acceptingResolveResults = true
        val discoveryFinished = CountDownLatch(1)
        val timeout = timeoutMillis.coerceAtLeast(0L)
        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout)

        lateinit var startResolve: (PendingResolution) -> Unit

        fun completeResolve(request: PendingResolution, record: MatterMdnsRecord?) {
            val nextService = synchronized(stateLock) {
                if (acceptingResolveResults) {
                    tracker.resolved(request.ticket, record)
                }

                val next = pendingServices.removeFirstOrNull()
                resolveInProgress = next != null
                if (!resolveInProgress) {
                    stateLock.notifyAll()
                }
                next
            }

            if (nextService != null) {
                startResolve(nextService)
            }
        }

        startResolve = { request ->
            resolve(
                manager = manager,
                requestedServiceType = serviceType,
                serviceInfo = request.serviceInfo,
                onComplete = { record -> completeResolve(request, record) }
            )
        }

        fun enqueueResolve(serviceInfo: NsdServiceInfo) {
            val serviceToResolve = synchronized(stateLock) {
                if (!acceptingResolveResults) return
                val ticket = tracker.found(serviceKey(serviceInfo)) ?: return
                val request = PendingResolution(serviceInfo, ticket)

                if (resolveInProgress) {
                    pendingServices.addLast(request)
                    null
                } else {
                    resolveInProgress = true
                    request
                }
            }

            if (serviceToResolve != null) {
                startResolve(serviceToResolve)
            }
        }

        fun waitForResolveIdle(timeoutMillis: Long) {
            if (timeoutMillis <= 0L) {
                return
            }

            val resolveDeadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
            synchronized(stateLock) {
                var remainingNanos = resolveDeadlineNanos - System.nanoTime()
                while (resolveInProgress && remainingNanos > 0L) {
                    stateLock.wait(TimeUnit.NANOSECONDS.toMillis(remainingNanos).coerceAtLeast(1L))
                    remainingNanos = resolveDeadlineNanos - System.nanoTime()
                }
            }
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                enqueueResolve(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                synchronized(stateLock) {
                    val key = serviceKey(serviceInfo)
                    tracker.lost(key)
                    pendingServices.removeAll { it.ticket.key == key }
                }
            }

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
            discoveryFinished.await(timeout, TimeUnit.MILLISECONDS)
            stopDiscovery(manager, listener)
            waitForResolveIdle(remainingMillis(deadlineNanos))
            synchronized(stateLock) {
                acceptingResolveResults = false
                pendingServices.clear()
                tracker.snapshot()
            }
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
        onComplete: (MatterMdnsRecord?) -> Unit
    ) {
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                onComplete(unresolvedRecord(serviceInfo, requestedServiceType, "Android NSD error $errorCode"))
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                onComplete(serviceInfo.toMatterMdnsRecord(requestedServiceType))
            }
        }

        try {
            manager.resolveService(serviceInfo, listener)
        } catch (e: SecurityException) {
            onComplete(unresolvedRecord(serviceInfo, requestedServiceType, "Android NSD security exception"))
        } catch (e: IllegalArgumentException) {
            onComplete(unresolvedRecord(serviceInfo, requestedServiceType, "Android NSD invalid service"))
        } catch (e: RuntimeException) {
            onComplete(unresolvedRecord(serviceInfo, requestedServiceType, "Android NSD resolution exception"))
        }
    }

    private fun unresolvedRecord(
        serviceInfo: NsdServiceInfo,
        requestedServiceType: String,
        error: String
    ): MatterMdnsRecord = MatterMdnsRecord(
        serviceType = requestedServiceType,
        instanceName = serviceInfo.serviceName,
        host = "",
        ipv6Addresses = emptyList(),
        port = 0,
        txt = emptyMap(),
        resolutionFailure = error
    )

    private fun serviceKey(serviceInfo: NsdServiceInfo): String =
        "${serviceInfo.serviceType}/${serviceInfo.serviceName}"

    private data class PendingResolution(
        val serviceInfo: NsdServiceInfo,
        val ticket: MatterMdnsServiceTracker.Ticket
    )

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

    private fun remainingMillis(deadlineNanos: Long): Long {
        val remainingNanos = deadlineNanos - System.nanoTime()
        if (remainingNanos <= 0L) {
            return 0L
        }
        return TimeUnit.NANOSECONDS.toMillis(remainingNanos).coerceAtLeast(1L)
    }

    private companion object {
        const val MATTER_COMMISSIONABLE_SERVICE_TYPE = "_matterc._udp"
        const val MATTER_OPERATIONAL_SERVICE_TYPE = "_matter._tcp"
    }
}
