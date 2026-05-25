package org.openhab.matter.companion;

import org.junit.Test;
import org.openhab.matter.companion.controller.ChipMatterControllerStatus;
import org.openhab.matter.companion.controller.ConnectedHomeIpControllerArtifactsStatus;
import org.openhab.matter.companion.controller.FakeMatterController;
import org.openhab.matter.companion.controller.MatterControllerSelection;
import org.openhab.matter.companion.openhab.OpenHabInboxStatus;
import org.openhab.matter.companion.otbr.OtbrStatus;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class MainActivityPresentationTest {
    @Test
    public void describesRuntimePermissionsNotNeeded() {
        assertEquals(
                "No runtime commissioning permissions are required on this Android version.",
                MainActivityPresentation.runtimePermissionsNotNeeded());
    }

    @Test
    public void describesRuntimePermissionsRequestedWithoutEchoingImplementationDetails() {
        assertEquals(
                "Requesting runtime commissioning permissions: android.permission.BLUETOOTH_SCAN, android.permission.BLUETOOTH_CONNECT",
                MainActivityPresentation.runtimePermissionsRequested(Arrays.asList(
                        "android.permission.BLUETOOTH_SCAN",
                        "android.permission.BLUETOOTH_CONNECT")));
    }

    @Test
    public void describesRuntimePermissionsAlreadyGranted() {
        assertEquals(
                "Runtime commissioning permissions already granted: android.permission.ACCESS_FINE_LOCATION",
                MainActivityPresentation.runtimePermissionsAlreadyGranted(Collections.singletonList(
                        "android.permission.ACCESS_FINE_LOCATION")));
    }

    @Test
    public void describesDetectedMatterInboxEntry() {
        OpenHabInboxStatus status = new OpenHabInboxStatus(
                true,
                true,
                "Matter Inbox entry detected",
                "HTTP 200 from http://openhab.local:8080/rest/inbox");

        assertEquals(
                "openHAB Inbox observation: Matter Inbox entry detected.",
                MainActivityPresentation.openHabInboxResult(status));
    }

    @Test
    public void describesMissingMatterInboxEntry() {
        OpenHabInboxStatus status = new OpenHabInboxStatus(
                true,
                false,
                "No Matter Inbox entry detected",
                "HTTP 200 from http://openhab.local:8080/rest/inbox");

        assertEquals(
                "openHAB Inbox observation: no Matter Inbox entry detected.",
                MainActivityPresentation.openHabInboxResult(status));
    }

    @Test
    public void describesUnreachableInboxSeparatelyFromMissingMatterEntry() {
        OpenHabInboxStatus status = new OpenHabInboxStatus(
                false,
                false,
                "openHAB Inbox is not reachable",
                "Connection refused");

        assertEquals(
                "openHAB Inbox observation failed: openHAB Inbox was not reachable.",
                MainActivityPresentation.openHabInboxResult(status));
    }

    @Test
    public void describesIncompletePermissionResultAsNotGranted() {
        assertEquals(
                "Runtime commissioning permission request completed: one or more permissions were denied or the request was interrupted.",
                MainActivityPresentation.runtimePermissionRequestResult(
                        new String[] {"android.permission.BLUETOOTH_SCAN"},
                        new int[] {}));
    }

    @Test
    public void describesGrantedPermissionResultOnlyWhenEachPermissionHasGrantedResult() {
        assertEquals(
                "Runtime commissioning permission request completed: all requested permissions granted.",
                MainActivityPresentation.runtimePermissionRequestResult(
                        new String[] {"android.permission.BLUETOOTH_SCAN"},
                        new int[] {0}));
    }

    @Test
    public void redactsSensitiveUrlPartsBeforeLogging() {
        assertEquals(
                "http://openhab.local:8080",
                MainActivityPresentation.safeUrlForLog("http://user:secret@openhab.local:8080?token=abc#frag"));
    }

    @Test
    public void redactsSensitiveUrlPartsInsideDetails() {
        assertEquals(
                "HTTP 200 from http://openhab.local:8080/rest/inbox",
                MainActivityPresentation.safeTextForLog(
                        "HTTP 200 from http://user:secret@openhab.local:8080/rest/inbox?token=abc#frag"));
    }

    @Test
    public void redactsNonHttpUrlPartsInsideDetails() {
        assertEquals(
                "Failed to reach ftp://otbr.local/path",
                MainActivityPresentation.safeTextForLog(
                        "Failed to reach ftp://user:secret@otbr.local/path?token=abc#frag"));
    }

    @Test
    public void redactsOpaqueUrlPartsInsideDetails() {
        assertEquals(
                "Failed to reach ssh:otbr.local",
                MainActivityPresentation.safeTextForLog(
                        "Failed to reach ssh:user:secret@otbr.local?token=abc#frag"));
    }

    @Test
    public void redactsHostlessUrlPartsInsideDetails() {
        assertEquals(
                "Failed to reach file:/tmp/otbr and urn:thing",
                MainActivityPresentation.safeTextForLog(
                        "Failed to reach file:/tmp/otbr?token=abc#frag and urn:thing?token=abc"));
    }

    @Test
    public void redactsHostlessUrlFragmentPartsInsideDetails() {
        assertEquals(
                "Failed to reach file:/tmp/otbr and urn:thing",
                MainActivityPresentation.safeTextForLog(
                        "Failed to reach file:/tmp/otbr#access_token=abc and urn:thing#token=abc"));
    }

    @Test
    public void redactsSensitiveUrlPartsWithUppercaseScheme() {
        assertEquals(
                "HTTP://openhab.local:8080",
                MainActivityPresentation.safeUrlForLog("HTTP://user:secret@openhab.local:8080?token=abc#frag"));
    }

    @Test
    public void redactsSensitiveUrlPartsForUnsupportedSchemes() {
        assertEquals(
                "ftp://otbr.local/path",
                MainActivityPresentation.safeUrlForLog("ftp://user:secret@otbr.local/path?token=abc#frag"));
    }

    @Test
    public void redactsSensitiveUrlPartsForHostlessUnsupportedSchemes() {
        assertEquals(
                "file:/tmp/otbr",
                MainActivityPresentation.safeUrlForLog("file:/tmp/otbr?token=abc#frag"));
    }

    @Test
    public void redactsUserInfoForOpaqueUnsupportedSchemes() {
        assertEquals(
                "ssh:otbr.local",
                MainActivityPresentation.safeUrlForLog("ssh:user:secret@otbr.local?token=abc#frag"));
    }

    @Test
    public void redactsSlashContainingUserInfoForOpaqueUnsupportedSchemes() {
        assertEquals(
                "ssh:otbr.local",
                MainActivityPresentation.safeUrlForLog("ssh:user:tok/en@otbr.local?token=abc#frag"));
    }

    @Test
    public void redactsUserInfoWhenUriParsingFails() {
        assertEquals(
                "ssh:otbr.local",
                MainActivityPresentation.safeUrlForLog("ssh:user:secret@otbr.local?token=%zz#frag"));
    }

    @Test
    public void redactsHostlessOpaqueUserInfoBeforePath() {
        assertEquals(
                "ssh:/tmp",
                MainActivityPresentation.safeUrlForLog("ssh:user:secret@/tmp?token=abc#frag"));
    }

    @Test
    public void preservesPathAtSignWhenUriParsingFails() {
        assertEquals(
                "http://openhab.local/path@v1",
                MainActivityPresentation.safeUrlForLog("http://openhab.local/path@v1?token=%zz#frag"));
    }

    @Test
    public void redactsMatterPinVariantsInsideDetails() {
        assertEquals(
                "Errors: pin=<redacted>; pin=<redacted>; pin=<redacted>",
                MainActivityPresentation.safeTextForLog(
                        "Errors: pin: 20202021; setup PIN 20202021; passcode 20202021"));
    }

    @Test
    public void redactsMatterPinCodeFieldVariantsInsideDetails() {
        assertEquals(
                "Errors: pin=<redacted>; pin=<redacted>; pin=<redacted>; pin=<redacted>",
                MainActivityPresentation.safeTextForLog(
                        "Errors: setupPinCode=20202021; pin_code=20202021; setup-passcode: 20202021; setup_pin_code 20202021"));
    }

    @Test
    public void describesEncryptedConfigSave() {
        assertEquals(
                "Saved Thread dataset in encrypted app storage, saved OTBR base URL, saved openHAB base URL, and saved developer attestation bypass: off. Setup payloads and PINs are not saved.",
                MainActivityPresentation.encryptedConfigSaved());
    }

    @Test
    public void describesAttestationBypassWarning() {
        assertEquals(
                "Developer attestation bypass skips device attestation verification for lab devices only. Leave it off for production pairing.",
                MainActivityPresentation.attestationBypassWarning());
    }

    @Test
    public void describesConfigSaveWithAttestationBypassDisabled() {
        assertEquals(
                "Saved Thread dataset in encrypted app storage, saved OTBR base URL, saved openHAB base URL, and saved developer attestation bypass: off. Setup payloads and PINs are not saved.",
                MainActivityPresentation.encryptedConfigSaved(false));
    }

    @Test
    public void describesConfigSaveWithAttestationBypassEnabled() {
        assertEquals(
                "Saved Thread dataset in encrypted app storage, saved OTBR base URL, saved openHAB base URL, and saved developer attestation bypass: on. Setup payloads and PINs are not saved.",
                MainActivityPresentation.encryptedConfigSaved(true));
    }

    @Test
    public void describesReachableOtbrConnectivity() {
        assertEquals(
                "OTBR connectivity: endpoint reachable.",
                MainActivityPresentation.otbrConnectivityResult(new OtbrStatus(
                        true,
                        "OTBR endpoint is reachable",
                        "HTTP 200 from http://otbr.local")));
    }

    @Test
    public void describesUnreachableOtbrConnectivity() {
        assertEquals(
                "OTBR connectivity failed: endpoint was not reachable.",
                MainActivityPresentation.otbrConnectivityResult(new OtbrStatus(
                        false,
                        "OTBR endpoint is not reachable",
                        "Connection refused")));
    }

    @Test
    public void warnsWhenEncryptedThreadDatasetCannotBeRead() {
        assertEquals(
                "Stored Thread dataset could not be decrypted. Paste and save the dataset again to continue.",
                MainActivityPresentation.threadDatasetUnreadable());
    }

    @Test
    public void warnsWhenBootstrapStateCannotBeRead() {
        assertEquals(
                "Stored Matter bootstrap controller state could not be decrypted. Re-run Thread commissioning before opening a commissioning window.",
                MainActivityPresentation.bootstrapStateUnreadable());
    }

    @Test
    public void describesMatterSseEvent() {
        assertEquals(
                "openHAB Inbox SSE: Matter Inbox entry detected.",
                MainActivityPresentation.openHabInboxSseEvent(true));
    }

    @Test
    public void describesNonMatterSseEvent() {
        assertEquals(
                "openHAB Inbox SSE: event received but no Matter Inbox entry detected yet.",
                MainActivityPresentation.openHabInboxSseEvent(false));
    }

    @Test
    public void describesExternalQrScannerMissing() {
        assertEquals(
                "No external QR scanner app is installed. Paste the Matter code manually or install a scanner that supports ZXing scan intents.",
                MainActivityPresentation.externalQrScannerMissing());
    }

    @Test
    public void describesExternalQrScannerTrustBoundary() {
        assertEquals(
                "This opens an installed external QR scanner app. Only continue if you trust that scanner with Matter setup codes.",
                MainActivityPresentation.externalQrScannerTrustNotice());
    }

    @Test
    public void describesInvalidExternalQrScannerResult() {
        assertEquals(
                "QR scanner returned text that is not a supported Matter setup payload.",
                MainActivityPresentation.invalidExternalQrScannerResult());
    }

    @Test
    public void describesCameraPermissionRequiredForInAppScanner() {
        assertEquals(
                "Camera permission is required for in-app Matter QR scanning.",
                MainActivityPresentation.cameraPermissionRequired());
    }

    @Test
    public void describesInvalidInAppQrScannerResult() {
        assertEquals(
                "In-app QR scanner did not return a supported Matter setup payload.",
                MainActivityPresentation.invalidInAppQrScannerResult());
    }

    @Test
    public void describesNativeChipReadiness() {
        ChipMatterControllerStatus status = new ChipMatterControllerStatus(false, "openhab_matter_chip", false, "missing");

        assertEquals(
                "Native CHIP controller not ready: missing. Developer attestation bypass: off.",
                MainActivityPresentation.nativeChipReadiness(status));
    }

    @Test
    public void describesNativeChipReadinessWithAttestationBypass() {
        ChipMatterControllerStatus status = new ChipMatterControllerStatus(
                true,
                "openhab_matter_chip",
                true,
                "connectedhomeip",
                true,
                "Native CHIP library loaded: openhab_matter_chip");

        assertEquals(
                "Native CHIP controller ready: openhab_matter_chip. Developer attestation bypass: on.",
                MainActivityPresentation.nativeChipReadiness(status));
    }

    @Test
    public void describesReadyConnectedHomeIpArtifacts() {
        ConnectedHomeIpControllerArtifactsStatus status = new ConnectedHomeIpControllerArtifactsStatus(
                true,
                "CHIPController",
                "connectedhomeip Android controller artifacts are ready");

        assertEquals(
                "connectedhomeip Android controller artifacts ready: CHIPController.",
                MainActivityPresentation.connectedHomeIpArtifacts(status));
    }

    @Test
    public void describesMissingConnectedHomeIpArtifacts() {
        ConnectedHomeIpControllerArtifactsStatus status = new ConnectedHomeIpControllerArtifactsStatus(
                false,
                "CHIPController",
                "Missing connectedhomeip controller class: chip.platform.AndroidChipPlatform");

        assertEquals(
                "connectedhomeip Android controller artifacts not ready: Missing connectedhomeip controller class: chip.platform.AndroidChipPlatform",
                MainActivityPresentation.connectedHomeIpArtifacts(status));
    }
    @Test
    public void describesMatterControllerSelection() {
        MatterControllerSelection selection = new MatterControllerSelection(
                new FakeMatterController(),
                true,
                "Using native CHIP controller: custom_chip");

        assertEquals(
                "Matter controller selection: Using native CHIP controller: custom_chip",
                MainActivityPresentation.matterControllerSelection(selection));
    }

    @Test
    public void describesMatterControllerFailureWithoutLeakingUrlSecrets() {
        assertEquals(
                "Matter controller operation failed: HTTP 500 from http://chip.local:5540",
                MainActivityPresentation.matterControllerOperationFailed(
                        "HTTP 500 from http://user:secret@chip.local:5540?token=abc"));
    }

    @Test
    public void describesMatterControllerFailureWithoutLeakingMatterSecrets() {
        assertEquals(
                "Matter controller operation failed: failed with pin=<redacted> code=<redacted-matter-code> dataset <redacted-thread-dataset>",
                MainActivityPresentation.matterControllerOperationFailed(
                        "failed with pin=20202021 code=3497-0112-332 dataset hex:0E080000000000010000"));
    }

    @Test
    public void describesMatterControllerFailureWithoutLeakingQrSetupPayload() {
        assertEquals(
                "Matter controller operation failed: parser failed for <redacted-matter-qr-payload>",
                MainActivityPresentation.matterControllerOperationFailed(
                        "parser failed for MT:Y.K9042C00KA0648G00"));
    }
}
