package org.openhab.matter.companion;

import org.junit.Test;
import org.openhab.matter.companion.controller.ChipMatterControllerStatus;
import org.openhab.matter.companion.controller.ConnectedHomeIpControllerArtifactsStatus;
import org.openhab.matter.companion.controller.ConnectedHomeIpFabricRestoreStatus;
import org.openhab.matter.companion.controller.ConnectedHomeIpRuntimePreflightStatus;
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
                "Requesting runtime commissioning permissions: android.permission.BLUETOOTH_SCAN, android.permission.BLUETOOTH_CONNECT, android.permission.ACCESS_FINE_LOCATION",
                MainActivityPresentation.runtimePermissionsRequested(Arrays.asList(
                        "android.permission.BLUETOOTH_SCAN",
                        "android.permission.BLUETOOTH_CONNECT",
                        "android.permission.ACCESS_FINE_LOCATION")));
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
    public void redactsAuthorizationHeadersAndTokenFieldsInsideDetails() {
        assertEquals(
                "HTTP 401 Authorization: <redacted> token=<redacted> api_token:<redacted>",
                MainActivityPresentation.safeTextForLog(
                        "HTTP 401 Authorization: Bearer oh.secret.token token=abc123 api_token:xyz789"));
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
                "Saved Thread dataset and Matter setup payload in encrypted app storage, saved OTBR address, saved openHAB base URL, and saved developer attestation bypass: off. openHAB REST API token configured: off. Setup payloads, PINs, and tokens are not printed.",
                MainActivityPresentation.encryptedConfigSaved());
    }

    @Test
    public void describesAttestationBypassWarning() {
        assertEquals(
                "Developer attestation bypass skips device attestation verification for lab devices only. Leave it off for production pairing.",
                MainActivityPresentation.attestationBypassWarning());
    }

    @Test
    public void describesSelectableControllerModeNotice() {
        assertEquals(
                "Controller mode: connectedhomeip is selected automatically when packaged and ready. If it is not ready, Thread commissioning and OpenCommissioningWindow stop instead of silently using simulation.",
                MainActivityPresentation.controllerModeNotice());
    }

    @Test
    public void describesControllerActionLabelsWithoutFakeOnlyWording() {
        assertEquals("Run Thread commissioning", MainActivityPresentation.threadCommissioningButtonLabel());
        assertEquals("Open commissioning window", MainActivityPresentation.openCommissioningWindowButtonLabel());
        assertEquals("Clear logs", MainActivityPresentation.clearLogsButtonLabel());
        assertEquals("Check connectedhomeip controller", MainActivityPresentation.checkControllerButtonLabel());
        assertEquals("Check connectedhomeip fabric restore", MainActivityPresentation.checkFabricRestoreButtonLabel());
        assertEquals("Use connectedhomeip controller if ready", MainActivityPresentation.useControllerButtonLabel());
        assertEquals("Show troubleshooting guide", MainActivityPresentation.troubleshootingGuideButtonLabel());
        assertEquals(
                "Developer attestation bypass for connectedhomeip commissioning",
                MainActivityPresentation.attestationBypassLabel());
    }

    @Test
    public void describesCommissionedNodeIdsInHexForUsers() {
        assertEquals(
                "Bootstrap Matter node id: 0x1C856AE5C5AA4C95",
                MainActivityPresentation.bootstrapNodeId(0x1C856AE5C5AA4C95L));
        assertEquals(
                "Opening commissioning window for Matter node 0x1C856AE5C5AA4C95.",
                MainActivityPresentation.openCommissioningWindowTarget(0x1C856AE5C5AA4C95L));
    }

    @Test
    public void describesConfigSaveWithAttestationBypassDisabled() {
        assertEquals(
                "Saved Thread dataset and Matter setup payload in encrypted app storage, saved OTBR address, saved openHAB base URL, and saved developer attestation bypass: off. openHAB REST API token configured: off. Setup payloads, PINs, and tokens are not printed.",
                MainActivityPresentation.encryptedConfigSaved(false));
    }

    @Test
    public void describesConfigSaveWithAttestationBypassEnabled() {
        assertEquals(
                "Saved Thread dataset and Matter setup payload in encrypted app storage, saved OTBR address, saved openHAB base URL, and saved developer attestation bypass: on. openHAB REST API token configured: off. Setup payloads, PINs, and tokens are not printed.",
                MainActivityPresentation.encryptedConfigSaved(true));
    }

    @Test
    public void describesConfigSaveWithOpenHabApiTokenConfigured() {
        assertEquals(
                "Saved Thread dataset and Matter setup payload in encrypted app storage, saved OTBR address, saved openHAB base URL, and saved developer attestation bypass: on. openHAB REST API token configured: on. Setup payloads, PINs, and tokens are not printed.",
                MainActivityPresentation.encryptedConfigSaved(true, true));
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
    public void describesAcceptedOtbrAddress() {
        assertEquals(
                "OTBR connectivity: address accepted.",
                MainActivityPresentation.otbrConnectivityResult(new OtbrStatus(
                        true,
                        "OTBR address is accepted",
                        "Address fd00::1 resolves to fd00:0:0:0:0:0:0:1")));
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
    public void describesInvalidOtbrAddress() {
        assertEquals(
                "OTBR connectivity failed: address was invalid.",
                MainActivityPresentation.otbrConnectivityResult(new OtbrStatus(
                        false,
                        "OTBR address is invalid",
                        "Unsupported protocol: file")));
    }

    @Test
    public void warnsWhenEncryptedThreadDatasetCannotBeRead() {
        assertEquals(
                "Stored Thread dataset could not be decrypted. Paste and save the dataset again to continue.",
                MainActivityPresentation.threadDatasetUnreadable());
    }

    @Test
    public void warnsWhenEncryptedSetupPayloadCannotBeRead() {
        assertEquals(
                "Stored Matter setup payload could not be decrypted. Paste and save the setup payload again to continue.",
                MainActivityPresentation.setupPayloadUnreadable());
    }

    @Test
    public void warnsWhenOpenHabApiTokenCannotBeRead() {
        assertEquals(
                "Stored openHAB REST API token could not be decrypted. Paste and save the token again to use authenticated REST calls.",
                MainActivityPresentation.openHabApiTokenUnreadable());
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
                "Native Matter controller not ready: missing. Developer attestation bypass: off.",
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
                "Native Matter controller ready: openhab_matter_chip. Developer attestation bypass: on.",
                MainActivityPresentation.nativeChipReadiness(status));
    }

    @Test
    public void describesReadyConnectedHomeIpRuntimePreflight() {
        assertEquals(
                "connectedhomeip runtime preflight ready: controller and BLE manager initialized.",
                MainActivityPresentation.connectedHomeIpRuntimePreflight(
                        new ConnectedHomeIpRuntimePreflightStatus(
                                true,
                                "controller and BLE manager initialized")));
    }

    @Test
    public void describesFailedConnectedHomeIpRuntimePreflightWithoutLeakingSecrets() {
        assertEquals(
                "connectedhomeip runtime preflight not ready: failed for pin=<redacted> and <redacted-matter-qr-payload>",
                MainActivityPresentation.connectedHomeIpRuntimePreflight(
                        new ConnectedHomeIpRuntimePreflightStatus(
                                false,
                                "failed for pin=20202021 and MT:TEST")));
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
                "connectedhomeip Android controller artifacts not ready: Missing connectedhomeip controller class: chip.platform.AndroidChipPlatform. This APK was built without the official connectedhomeip CHIPTool-style controller artifacts. Rebuild and install with -ChipControllerArtifactsDir <artifact-dir> containing CHIPController.jar and jniLibs/<abi>/libCHIPController.so.",
                MainActivityPresentation.connectedHomeIpArtifacts(status));
    }

    @Test
    public void describesSkippedConnectedHomeIpFabricRestore() {
        assertEquals(
                "connectedhomeip fabric restore: no bootstrap fabric has been commissioned yet.",
                MainActivityPresentation.connectedHomeIpFabricRestore(
                        new ConnectedHomeIpFabricRestoreStatus(
                                false,
                                false,
                                -1L,
                                "No connectedhomeip bootstrap fabric has been commissioned yet.")));
    }

    @Test
    public void describesReadyConnectedHomeIpFabricRestore() {
        assertEquals(
                "connectedhomeip fabric restore ready for node 987654321.",
                MainActivityPresentation.connectedHomeIpFabricRestore(
                        new ConnectedHomeIpFabricRestoreStatus(
                                true,
                                true,
                                987654321L,
                                "connectedhomeip fabric restore ready for node 987654321.")));
    }

    @Test
    public void describesFailedConnectedHomeIpFabricRestoreWithoutLeakingSecrets() {
        assertEquals(
                "connectedhomeip fabric restore not ready: failed for pin=<redacted> and <redacted-matter-code>",
                MainActivityPresentation.connectedHomeIpFabricRestore(
                        new ConnectedHomeIpFabricRestoreStatus(
                                true,
                                false,
                                987654321L,
                                "failed for pin=20202021 and 3497-0112-332")));
    }

    @Test
    public void describesMatterControllerSelection() {
        MatterControllerSelection selection = new MatterControllerSelection(
                new FakeMatterController(),
                true,
                "Using native Matter controller: custom_chip");

        assertEquals(
                "Matter controller selection: Using native Matter controller: custom_chip",
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

    @Test
    public void describesOperationalIpv6RouteFailure() {
        assertEquals(
                "Matter controller operation failed: src/inet/UDPEndPointImplSockets.cpp:417: "
                        + "OS Error 0x02000065: Network is unreachable\n"
                        + "Operational IPv6 route is unreachable from this phone. Matter DNS-SD found the Thread node, "
                        + "but Android cannot route to its IPv6 address. Check that the OTBR advertises the Thread OMR "
                        + "prefix on Wi-Fi/LAN and that this phone has an IPv6 route to that prefix.",
                MainActivityPresentation.matterControllerOperationFailed(
                        "src/inet/UDPEndPointImplSockets.cpp:417: OS Error 0x02000065: Network is unreachable"));
    }
}
