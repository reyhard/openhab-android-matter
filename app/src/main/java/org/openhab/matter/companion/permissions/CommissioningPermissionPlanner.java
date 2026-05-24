package org.openhab.matter.companion.permissions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CommissioningPermissionPlanner {
    private static final List<String> BLUETOOTH_PERMISSIONS = Collections.unmodifiableList(Arrays.asList(
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT"));
    private static final List<String> LOCATION_PERMISSIONS = Collections.singletonList("android.permission.ACCESS_FINE_LOCATION");

    private CommissioningPermissionPlanner() {
    }

    public static List<String> requiredPermissions(int sdkVersion) {
        if (sdkVersion >= 31) {
            return BLUETOOTH_PERMISSIONS;
        }
        if (sdkVersion >= 23) {
            return LOCATION_PERMISSIONS;
        }
        return Collections.emptyList();
    }
}