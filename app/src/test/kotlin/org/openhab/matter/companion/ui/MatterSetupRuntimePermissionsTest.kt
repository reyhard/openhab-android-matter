package org.openhab.matter.companion.ui

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

class MatterSetupRuntimePermissionsTest {
    @Test
    fun android12SetupRequestsNearbyDevicesAndFineLocation() {
        val permissions = MatterSetupRuntimePermissions.requiredForSetup(Build.VERSION_CODES.S)

        assertEquals(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ),
            permissions
        )
    }

    @Test
    fun preAndroid12SetupRequestsFineLocationOnly() {
        val permissions = MatterSetupRuntimePermissions.requiredForSetup(Build.VERSION_CODES.R)

        assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION), permissions)
    }
}
