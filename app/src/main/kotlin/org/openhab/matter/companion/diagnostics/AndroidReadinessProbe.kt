package org.openhab.matter.companion.diagnostics

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat

class AndroidReadinessProbe(
    private val context: Context
) {
    @Suppress("DEPRECATION")
    fun bluetoothDiagnostic(): ReadinessDiagnostic {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return ReadinessDiagnostic(
                name = "Bluetooth",
                ok = false,
                message = "Bluetooth is unavailable on this device."
            )

        return try {
            if (adapter.isEnabled) {
                ReadinessDiagnostic(
                    name = "Bluetooth",
                    ok = true,
                    message = "Bluetooth is enabled."
                )
            } else {
                ReadinessDiagnostic(
                    name = "Bluetooth",
                    ok = false,
                    message = "Turn on Bluetooth before adding Matter devices."
                )
            }
        } catch (e: SecurityException) {
            ReadinessDiagnostic(
                name = "Bluetooth",
                ok = false,
                message = "Bluetooth permission is required."
            )
        }
    }

    fun locationServicesDiagnostic(): ReadinessDiagnostic {
        val locationManager = context.getSystemService(LocationManager::class.java)
            ?: return ReadinessDiagnostic(
                name = "Location services",
                ok = false,
                message = "Location services are unavailable on this device."
            )

        val enabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }

        return if (enabled) {
            ReadinessDiagnostic(
                name = "Location services",
                ok = true,
                message = "Location services are enabled."
            )
        } else {
            ReadinessDiagnostic(
                name = "Location services",
                ok = false,
                message = "Turn on location services before scanning for Matter devices."
            )
        }
    }

    fun permissionDiagnostics(): List<ReadinessDiagnostic> {
        val requiredPermissions = buildList {
            add(Manifest.permission.CAMERA to "Camera permission")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN to "Bluetooth scan permission")
                add(Manifest.permission.BLUETOOTH_CONNECT to "Bluetooth connect permission")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                add(Manifest.permission.ACCESS_FINE_LOCATION to "Fine location permission")
            }
        }

        return requiredPermissions.map { (permission, name) ->
            val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            ReadinessDiagnostic(
                name = name,
                ok = granted,
                message = if (granted) "Granted." else "Required for Matter setup."
            )
        }
    }

    fun networkTransportSummary(): NetworkTransportSummary {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)

        return NetworkTransportSummary(
            wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
            cellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
            vpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        )
    }

    private fun NetworkCapabilities?.hasTransport(transportType: Int): Boolean {
        return this?.hasTransport(transportType) == true
    }
}
