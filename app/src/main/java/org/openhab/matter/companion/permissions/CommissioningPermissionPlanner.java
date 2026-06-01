package org.openhab.matter.companion.permissions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CommissioningPermissionPlanner {
    private static final String CAMERA_PERMISSION = "android.permission.CAMERA";
    private static final List<String> BLUETOOTH_PERMISSIONS = Collections.unmodifiableList(Arrays.asList(
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT"));
    private static final List<String> CAMERA_AND_LOCATION_PERMISSIONS = Collections.unmodifiableList(Arrays.asList(
            CAMERA_PERMISSION,
            "android.permission.ACCESS_FINE_LOCATION"));
    private static final List<String> CAMERA_BLUETOOTH_AND_LOCATION_PERMISSIONS = Collections.unmodifiableList(Arrays.asList(
            CAMERA_PERMISSION,
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.ACCESS_FINE_LOCATION"));

    private CommissioningPermissionPlanner() {
    }

    public static List<String> requiredPermissions(int sdkVersion) {
        if (sdkVersion >= 31) {
            return CAMERA_BLUETOOTH_AND_LOCATION_PERMISSIONS;
        }
        if (sdkVersion >= 23) {
            return CAMERA_AND_LOCATION_PERMISSIONS;
        }
        return Collections.emptyList();
    }
}
