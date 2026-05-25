package org.openhab.matter.companion;

import org.openhab.matter.companion.controller.ChipMatterControllerStatus;
import org.openhab.matter.companion.controller.ConnectedHomeIpControllerArtifactsStatus;
import org.openhab.matter.companion.controller.ConnectedHomeIpFabricRestoreStatus;
import org.openhab.matter.companion.controller.MatterControllerSelection;
import org.openhab.matter.companion.openhab.OpenHabInboxStatus;
import org.openhab.matter.companion.otbr.OtbrStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MainActivityPresentation {
    private static final int PERMISSION_GRANTED = 0;
    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\b[A-Za-z][A-Za-z0-9+.-]*:(?://\\S+|\\S*[?@#]\\S*)");
    private static final Pattern MATTER_QR_PAYLOAD_PATTERN = Pattern.compile("\\bMT:[0-9A-Z.-]+\\b");
    private static final Pattern MATTER_PIN_PATTERN = Pattern.compile(
            "(?i)\\b(?:pin(?:[-_\\s]?code)?|setup[-_\\s]?(?:pin(?:[-_\\s]?code)?|passcode)|passcode)"
                    + "\\s*(?:=|:)?\\s*\\d{8}\\b");
    private static final Pattern MATTER_MANUAL_CODE_PATTERN = Pattern.compile("\\b(?:\\d{4}-\\d{4}-\\d{3}|\\d{11})\\b");
    private static final Pattern THREAD_DATASET_PATTERN = Pattern.compile("(?i)\\b(?:hex:)?[0-9a-f]{16,}\\b");

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

    static String encryptedConfigSaved() {
        return encryptedConfigSaved(false);
    }

    static String encryptedConfigSaved(boolean attestationBypassEnabled) {
        return "Saved Thread dataset in encrypted app storage, saved OTBR base URL, saved openHAB base URL, and saved developer attestation bypass: "
                + onOff(attestationBypassEnabled)
                + ". Setup payloads and PINs are not saved.";
    }

    static String attestationBypassWarning() {
        return "Developer attestation bypass skips device attestation verification for lab devices only. Leave it off for production pairing.";
    }

    static String controllerModeNotice() {
        return "Controller mode: simulated fallback is used by default. If connectedhomeip Android controller artifacts are bundled and readiness passes, the app can attempt real BLE Thread commissioning and OpenCommissioningWindow; Matter-over-Thread hardware, OTBR, and openHAB validation are still required.";
    }

    static String threadCommissioningButtonLabel() {
        return "Run Thread commissioning";
    }

    static String openCommissioningWindowButtonLabel() {
        return "Open commissioning window";
    }

    static String checkControllerButtonLabel() {
        return "Check connectedhomeip controller";
    }

    static String checkFabricRestoreButtonLabel() {
        return "Check connectedhomeip fabric restore";
    }

    static String useControllerButtonLabel() {
        return "Use connectedhomeip controller if ready";
    }

    static String attestationBypassLabel() {
        return "Developer attestation bypass for connectedhomeip commissioning";
    }

    static String threadDatasetUnreadable() {
        return "Stored Thread dataset could not be decrypted. Paste and save the dataset again to continue.";
    }

    static String bootstrapStateUnreadable() {
        return "Stored Matter bootstrap controller state could not be decrypted. Re-run Thread commissioning before opening a commissioning window.";
    }

    static String openHabInboxSseEvent(boolean matterEntryDetected) {
        if (matterEntryDetected) {
            return "openHAB Inbox SSE: Matter Inbox entry detected.";
        }
        return "openHAB Inbox SSE: event received but no Matter Inbox entry detected yet.";
    }

    static String otbrConnectivityResult(OtbrStatus status) {
        if (status.reachable()) {
            return "OTBR connectivity: endpoint reachable.";
        }
        return "OTBR connectivity failed: endpoint was not reachable.";
    }

    static String externalQrScannerMissing() {
        return "No external QR scanner app is installed. Paste the Matter code manually or install a scanner that supports ZXing scan intents.";
    }

    static String externalQrScannerTrustNotice() {
        return "This opens an installed external QR scanner app. Only continue if you trust that scanner with Matter setup codes.";
    }

    static String invalidExternalQrScannerResult() {
        return "QR scanner returned text that is not a supported Matter setup payload.";
    }

    static String cameraPermissionRequired() {
        return "Camera permission is required for in-app Matter QR scanning.";
    }

    static String invalidInAppQrScannerResult() {
        return "In-app QR scanner did not return a supported Matter setup payload.";
    }

    static String nativeChipReadiness(ChipMatterControllerStatus status) {
        if (status.ready()) {
            return "Native Matter controller ready: " + status.libraryName()
                    + ". Developer attestation bypass: " + onOff(status.attestationBypassEnabled()) + ".";
        }
        return "Native Matter controller not ready: " + status.message()
                + ". Developer attestation bypass: " + onOff(status.attestationBypassEnabled()) + ".";
    }

    static String connectedHomeIpArtifacts(ConnectedHomeIpControllerArtifactsStatus status) {
        if (status.ready()) {
            return "connectedhomeip Android controller artifacts ready: " + status.libraryName() + ".";
        }
        return "connectedhomeip Android controller artifacts not ready: " + status.message();
    }

    static String connectedHomeIpFabricRestore(ConnectedHomeIpFabricRestoreStatus status) {
        if (!status.checked()) {
            return "connectedhomeip fabric restore: no bootstrap fabric has been commissioned yet.";
        }
        if (status.ready()) {
            return "connectedhomeip fabric restore ready for node " + status.nodeId() + ".";
        }
        return "connectedhomeip fabric restore not ready: " + safeTextForLog(status.message());
    }

    static String matterControllerSelection(MatterControllerSelection selection) {
        return "Matter controller selection: " + selection.message();
    }

    static String matterControllerOperationFailed(String message) {
        return "Matter controller operation failed: " + safeTextForLog(message);
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
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                return sanitizeUrlFallback(trimmed);
            }
            String path = uri.getRawPath();
            return new URI(scheme, null, uri.getHost(), uri.getPort(), path, null, null).toString();
        } catch (URISyntaxException | IllegalArgumentException ex) {
            return sanitizeUrlFallback(trimmed);
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
        return redactMatterSecrets(sanitized.toString());
    }

    private static String joinPermissions(List<String> permissions) {
        return String.join(", ", permissions);
    }

    private static String runtimePermissionRequestDeniedOrInterrupted() {
        return "Runtime commissioning permission request completed: one or more permissions were denied or the request was interrupted.";
    }

    private static String onOff(boolean enabled) {
        return enabled ? "on" : "off";
    }

    private static String redactMatterSecrets(String value) {
        String redacted = MATTER_QR_PAYLOAD_PATTERN.matcher(value).replaceAll("<redacted-matter-qr-payload>");
        redacted = MATTER_PIN_PATTERN.matcher(redacted).replaceAll("pin=<redacted>");
        redacted = MATTER_MANUAL_CODE_PATTERN.matcher(redacted).replaceAll("<redacted-matter-code>");
        return THREAD_DATASET_PATTERN.matcher(redacted).replaceAll("<redacted-thread-dataset>");
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

    private static String sanitizeUrlFallback(String value) {
        String stripped = stripQueryAndFragment(value);
        int authorityStart = stripped.indexOf("://");
        if (authorityStart >= 0) {
            int hostStart = authorityStart + 3;
            int pathStart = stripped.indexOf('/', hostStart);
            int authorityEnd = pathStart >= 0 ? pathStart : stripped.length();
            int userInfoEnd = stripped.lastIndexOf('@', authorityEnd - 1);
            if (userInfoEnd >= hostStart) {
                return stripped.substring(0, hostStart) + stripped.substring(userInfoEnd + 1);
            }
            return stripped;
        }
        return stripped.replaceFirst("^([A-Za-z][A-Za-z0-9+.-]*:).*@(.*)$", "$1$2");
    }
}
