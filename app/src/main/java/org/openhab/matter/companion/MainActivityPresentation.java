package org.openhab.matter.companion;

import org.openhab.matter.companion.openhab.OpenHabInboxStatus;

import java.util.List;

final class MainActivityPresentation {
    private MainActivityPresentation() {
    }

    static String runtimePermissionsNotNeeded() {
        return "No runtime commissioning permissions are required on this Android version.";
    }

    static String runtimePermissionsRequested(List<String> permissions) {
        return "Requesting runtime commissioning permissions: " + joinPermissions(permissions);
    }

    static String runtimePermissionsAlreadyGranted(List<String> permissions) {
        return "Runtime commissioning permissions already granted: " + joinPermissions(permissions);
    }

    static String openHabInboxResult(OpenHabInboxStatus status) {
        if (status.matterEntryDetected()) {
            return "openHAB Inbox observation: Matter Inbox entry detected.";
        }
        return "openHAB Inbox observation: no Matter Inbox entry detected.";
    }

    private static String joinPermissions(List<String> permissions) {
        return String.join(", ", permissions);
    }
}
