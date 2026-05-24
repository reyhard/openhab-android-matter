package org.openhab.matter.companion.permissions;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CommissioningPermissionPlannerTest {
    @Test
    public void sdk31RequiresBluetoothRuntimePermissions() {
        assertEquals(
                Arrays.asList("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT"),
                CommissioningPermissionPlanner.requiredPermissions(31));
    }

    @Test
    public void sdk30RequiresFineLocationPermission() {
        assertEquals(
                Collections.singletonList("android.permission.ACCESS_FINE_LOCATION"),
                CommissioningPermissionPlanner.requiredPermissions(30));
    }

    @Test
    public void sdk22RequiresNoRuntimePermissions() {
        assertEquals(
                Collections.emptyList(),
                CommissioningPermissionPlanner.requiredPermissions(22));
    }

    @Test
    public void returnedPermissionsCannotMutateSharedState() {
        List<String> permissions = CommissioningPermissionPlanner.requiredPermissions(31);

        try {
            permissions.clear();
        } catch (UnsupportedOperationException ignored) {
            // Immutable return values are acceptable; defensive mutable copies are also acceptable.
        }

        assertEquals(
                Arrays.asList("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT"),
                CommissioningPermissionPlanner.requiredPermissions(31));
    }
}