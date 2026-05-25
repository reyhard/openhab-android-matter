package org.openhab.matter.companion.controller;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectedHomeIpControllerArtifactsTest {
    @Test
    public void reportsReadyWhenRequiredClassesAndNativeLibraryArePresent() {
        RecordingLoader loader = new RecordingLoader();
        ConnectedHomeIpControllerArtifacts artifacts = new ConnectedHomeIpControllerArtifacts(
                name -> true,
                loader);

        ConnectedHomeIpControllerArtifactsStatus status = artifacts.check();

        assertTrue(status.ready());
        assertEquals("CHIPController", loader.loadedLibraryName);
    }

    @Test
    public void checksExactlyTheRequiredConnectedHomeIpControllerClasses() {
        List<String> checkedClassNames = new ArrayList<>();
        ConnectedHomeIpControllerArtifacts artifacts = new ConnectedHomeIpControllerArtifacts(
                name -> {
                    checkedClassNames.add(name);
                    return true;
                },
                name -> {
                });

        ConnectedHomeIpControllerArtifactsStatus status = artifacts.check();

        assertTrue(status.ready());
        assertEquals(Arrays.asList(
                "chip.devicecontroller.ChipDeviceController",
                "chip.devicecontroller.ControllerParams",
                "chip.devicecontroller.NetworkCredentials",
                "chip.devicecontroller.NetworkCredentials$ThreadCredentials",
                "chip.devicecontroller.CommissionParameters",
                "chip.devicecontroller.DeviceAttestationDelegate",
                "chip.devicecontroller.OpenCommissioningCallback",
                "chip.devicecontroller.GetConnectedDeviceCallbackJni$GetConnectedDeviceCallback",
                "chip.platform.AndroidChipPlatform",
                "chip.platform.AndroidBleManager",
                "chip.platform.AndroidNfcCommissioningManager",
                "chip.platform.PreferencesKeyValueStoreManager",
                "chip.platform.PreferencesConfigurationManager",
                "chip.platform.NsdManagerServiceResolver",
                "chip.platform.NsdManagerServiceBrowser",
                "chip.platform.ChipMdnsCallbackImpl",
                "chip.platform.DiagnosticDataProviderImpl"), checkedClassNames);
    }
    @Test
    public void reportsMissingClass() {
        ConnectedHomeIpControllerArtifacts artifacts = new ConnectedHomeIpControllerArtifacts(
                name -> !"chip.platform.AndroidChipPlatform".equals(name),
                name -> {
                });

        ConnectedHomeIpControllerArtifactsStatus status = artifacts.check();

        assertFalse(status.ready());
        assertTrue(status.message().contains("chip.platform.AndroidChipPlatform"));
    }

    @Test
    public void reportsNativeLoadFailure() {
        ConnectedHomeIpControllerArtifacts artifacts = new ConnectedHomeIpControllerArtifacts(
                name -> true,
                name -> {
                    throw new UnsatisfiedLinkError("missing");
                });

        ConnectedHomeIpControllerArtifactsStatus status = artifacts.check();

        assertFalse(status.ready());
        assertTrue(status.message().contains("libCHIPController"));
    }

    private static final class RecordingLoader implements NativeLibraryLoader {
        private String loadedLibraryName;

        @Override
        public void load(String libraryName) {
            loadedLibraryName = libraryName;
        }
    }
}
