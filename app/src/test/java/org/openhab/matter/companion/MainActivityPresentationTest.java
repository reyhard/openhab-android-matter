package org.openhab.matter.companion;

import org.junit.Test;
import org.openhab.matter.companion.openhab.OpenHabInboxStatus;

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
    public void redactsSensitiveUrlPartsWithUppercaseScheme() {
        assertEquals(
                "HTTP://openhab.local:8080",
                MainActivityPresentation.safeUrlForLog("HTTP://user:secret@openhab.local:8080?token=abc#frag"));
    }
}
