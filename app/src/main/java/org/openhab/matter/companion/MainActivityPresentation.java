package org.openhab.matter.companion;

import org.openhab.matter.companion.openhab.OpenHabInboxStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MainActivityPresentation {
    private static final int PERMISSION_GRANTED = 0;
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

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
        if (!status.reachable()) {
            return "openHAB Inbox observation failed: openHAB Inbox was not reachable.";
        }
        if (status.matterEntryDetected()) {
            return "openHAB Inbox observation: Matter Inbox entry detected.";
        }
        return "openHAB Inbox observation: no Matter Inbox entry detected.";
    }

    static String runtimePermissionRequestResult(String[] permissions, int[] grantResults) {
        if (permissions == null || grantResults == null || permissions.length == 0 || permissions.length != grantResults.length) {
            return runtimePermissionRequestDeniedOrInterrupted();
        }
        for (int result : grantResults) {
            if (result != PERMISSION_GRANTED) {
                return runtimePermissionRequestDeniedOrInterrupted();
            }
        }
        return "Runtime commissioning permission request completed: all requested permissions granted.";
    }

    static String safeUrlForLog(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        try {
            URI uri = new URI(trimmed);
            if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
                return trimmed;
            }
            String path = uri.getRawPath();
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), path, null, null).toString();
        } catch (URISyntaxException | IllegalArgumentException ex) {
            return stripQueryAndFragment(trimmed).replaceFirst("://[^/@]+@", "://");
        }
    }

    static String safeTextForLog(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        Matcher matcher = URL_PATTERN.matcher(value);
        StringBuffer sanitized = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sanitized, Matcher.quoteReplacement(safeUrlForLog(matcher.group())));
        }
        matcher.appendTail(sanitized);
        return sanitized.toString();
    }

    private static String joinPermissions(List<String> permissions) {
        return String.join(", ", permissions);
    }

    private static String runtimePermissionRequestDeniedOrInterrupted() {
        return "Runtime commissioning permission request completed: one or more permissions were denied or the request was interrupted.";
    }

    private static String stripQueryAndFragment(String value) {
        int queryIndex = value.indexOf('?');
        int fragmentIndex = value.indexOf('#');
        int endIndex = value.length();
        if (queryIndex >= 0) {
            endIndex = Math.min(endIndex, queryIndex);
        }
        if (fragmentIndex >= 0) {
            endIndex = Math.min(endIndex, fragmentIndex);
        }
        return value.substring(0, endIndex);
    }
}
