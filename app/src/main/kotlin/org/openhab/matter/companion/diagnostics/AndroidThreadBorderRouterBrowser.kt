package org.openhab.matter.companion.diagnostics

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class AndroidThreadBorderRouterBrowser(
    context: Context
) : ThreadBorderRouterBrowser {
    private val nsdManager: NsdManager? = context.getSystemService(NsdManager::class.java)

    override fun browse(timeoutMillis: Long): List<ThreadBorderRouterRecord> {
        val manager = nsdManager ?: return emptyList()
        val records = mutableListOf<ThreadBorderRouterRecord>()
        val seenServices = mutableSetOf<String>()
        val stateLock = Object()
        val discoveryFinished = CountDownLatch(1)
        val timeout = timeoutMillis.coerceAtLeast(0L)

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val key = "${serviceInfo.serviceType}/${serviceInfo.serviceName}"
                synchronized(stateLock) {
                    if (!seenServices.add(key)) {
                        return
                    }
                }
                resolve(manager, serviceInfo) { record ->
                    if (record != null) {
                        synchronized(stateLock) {
                            records.add(record)
                        }
                    }
                }
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
            manager.discoverServices(THREAD_BORDER_AGENT_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            discoveryFinished.await(timeout, TimeUnit.MILLISECONDS)
            stopDiscovery(manager, listener)
            Thread.sleep(RESOLVE_SETTLE_MILLIS.coerceAtMost(timeout))
            synchronized(stateLock) {
                records.toList()
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
        serviceInfo: NsdServiceInfo,
        onComplete: (ThreadBorderRouterRecord?) -> Unit
    ) {
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                onComplete(null)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                onComplete(
                    ThreadBorderRouterRecord(
                        instanceName = serviceInfo.serviceName,
                        host = serviceInfo.host?.hostName.orEmpty(),
                        addresses = serviceInfo.host?.hostAddress.orEmpty().takeIf { it.isNotBlank() }?.let(::listOf)
                            ?: emptyList(),
                        port = serviceInfo.port
                    )
                )
            }
        }

        try {
            manager.resolveService(serviceInfo, listener)
        } catch (e: SecurityException) {
            onComplete(null)
        } catch (e: IllegalArgumentException) {
            onComplete(null)
        } catch (e: RuntimeException) {
            onComplete(null)
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

    private companion object {
        const val THREAD_BORDER_AGENT_SERVICE_TYPE = "_meshcop._udp"
        const val RESOLVE_SETTLE_MILLIS = 500L
    }
}
