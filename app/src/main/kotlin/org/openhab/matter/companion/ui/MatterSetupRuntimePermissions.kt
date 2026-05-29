package org.openhab.matter.companion.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build

object MatterSetupRuntimePermissions {
    fun requiredForSetup(sdkInt: Int = Build.VERSION.SDK_INT): List<String> {
        return buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (sdkInt >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }

    fun missingForSetup(activity: Activity): List<String> {
        return requiredForSetup().filter { permission ->
            activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun locationServicesEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(LocationManager::class.java)
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
