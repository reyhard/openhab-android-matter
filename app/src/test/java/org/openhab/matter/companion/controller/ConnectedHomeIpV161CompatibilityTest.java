package org.openhab.matter.companion.controller;

import android.bluetooth.BluetoothGatt;
import android.content.Context;

import org.junit.Assume;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Compile-free signature check against the connectedhomeip jars actually packaged on the test
 * classpath. It is skipped in stub/offline builds where real controller artifacts are absent.
 */
public final class ConnectedHomeIpV161CompatibilityTest {
    private static final String CHIP_DEVICE_CONTROLLER_CLASS = "chip.devicecontroller.ChipDeviceController";

    @Test
    public void reflectionSeamsMatchPackagedControllerArtifacts() throws Exception {
        Assume.assumeTrue(
                "connectedhomeip controller jars are not on the test classpath",
                isPresent(CHIP_DEVICE_CONTROLLER_CLASS));

        Class<?> chipDeviceController = loadClass(CHIP_DEVICE_CONTROLLER_CLASS);
        Class<?> controllerParams = loadClass("chip.devicecontroller.ControllerParams");
        Class<?> commissionParameters = loadClass("chip.devicecontroller.CommissionParameters");
        Class<?> openCommissioningCallback = loadClass("chip.devicecontroller.OpenCommissioningCallback");
        Class<?> icdRegistrationInfo = loadClass("chip.devicecontroller.ICDRegistrationInfo");
        Class<?> icdDeviceInfo = loadClass("chip.devicecontroller.ICDDeviceInfo");
        Class<?> completionListener = loadClass(CHIP_DEVICE_CONTROLLER_CLASS + "$CompletionListener");
        Class<?> getConnectedDeviceCallback =
                loadClass("chip.devicecontroller.GetConnectedDeviceCallbackJni$GetConnectedDeviceCallback");
        Class<?> attestationTrustStoreDelegate =
                loadClass("chip.devicecontroller.AttestationTrustStoreDelegate");
        Class<?> deviceAttestationDelegate =
                loadClass("chip.devicecontroller.DeviceAttestationDelegate");
        Class<?> attestationInfo = loadClass("chip.devicecontroller.AttestationInfo");
        Class<?> androidChipPlatform = loadClass("chip.platform.AndroidChipPlatform");
        Class<?> androidBleManager = loadClass("chip.platform.AndroidBleManager");

        assertNotNull(chipDeviceController.getMethod("loadJni"));
        assertNotNull(chipDeviceController.getConstructor(controllerParams));
        assertNotNull(controllerParams.getMethod("newBuilder"));
        Object paramsBuilder = controllerParams.getMethod("newBuilder").invoke(null);
        assertNotNull(paramsBuilder.getClass().getMethod("setControllerVendorId", int.class));
        assertNotNull(paramsBuilder.getClass().getMethod("setEnableServerInteractions", boolean.class));
        assertNotNull(paramsBuilder.getClass().getMethod("build"));

        assertNotNull(chipDeviceController.getMethod(
                "pairDeviceThroughBLE",
                BluetoothGatt.class,
                int.class,
                long.class,
                long.class,
                commissionParameters));
        assertNotNull(chipDeviceController.getMethod(
                "openPairingWindowWithPINCallback",
                long.class,
                int.class,
                long.class,
                int.class,
                Long.class,
                openCommissioningCallback));
        assertNotNull(chipDeviceController.getMethod(
                "updateCommissioningICDRegistrationInfo",
                icdRegistrationInfo));
        assertNotNull(chipDeviceController.getMethod(
                "getConnectedDevicePointer",
                long.class,
                getConnectedDeviceCallback));
        assertNotNull(chipDeviceController.getMethod("releaseConnectedDevicePointer", long.class));
        assertNotNull(chipDeviceController.getMethod(
                "setAttestationTrustStoreDelegate",
                attestationTrustStoreDelegate,
                List.class));

        assertNotNull(completionListener.getMethod("onCommissioningComplete", long.class, long.class));
        assertNotNull(completionListener.getMethod("onCommissioningStatusUpdate", long.class, String.class, long.class));
        assertNotNull(completionListener.getMethod("onCommissioningStageStart", long.class, String.class));
        assertNotNull(completionListener.getMethod("onICDRegistrationInfoRequired"));
        assertNotNull(completionListener.getMethod("onICDRegistrationComplete", long.class, icdDeviceInfo));

        assertNotNull(icdRegistrationInfo.getMethod("createForDeferredConfiguration"));
        assertNotNull(icdRegistrationInfo.getMethod("newBuilder"));
        assertNotNull(icdRegistrationInfo.getMethod("getICDStayActiveDurationMsec"));
        Object builder = icdRegistrationInfo.getMethod("newBuilder").invoke(null);
        assertNotNull(builder.getClass().getMethod("setICDStayActiveDurationMsec", Long.class));
        Class<?> commissionParametersBuilder = loadClass("chip.devicecontroller.CommissionParameters$Builder");
        assertNotNull(commissionParametersBuilder.getMethod("setICDRegistrationInfo", icdRegistrationInfo));
        assertNotNull(commissionParametersBuilder.getMethod("setNetworkCredentials",
                loadClass("chip.devicecontroller.NetworkCredentials")));

        assertNotNull(openCommissioningCallback.getMethod("onError", int.class, long.class));
        assertNotNull(openCommissioningCallback.getMethod(
                "onSuccess", long.class, String.class, String.class));
        assertEquals(2, openCommissioningCallback.getDeclaredMethods().length);

        assertNotNull(deviceAttestationDelegate.getMethod(
                "onDeviceAttestationCompleted",
                long.class,
                attestationInfo,
                long.class));
        assertNotNull(attestationTrustStoreDelegate.getMethod(
                "getProductAttestationAuthorityCert",
                byte[].class));
        assertNotNull(chipDeviceController.getMethod(
                "setDeviceAttestationDelegate",
                int.class,
                deviceAttestationDelegate));
        assertNotNull(chipDeviceController.getMethod(
                "continueCommissioning",
                long.class,
                boolean.class));

        assertEquals(1L, java.util.Arrays.stream(androidChipPlatform.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == 8)
                .count());
        assertNotNull(androidBleManager.getConstructor(Context.class));
    }

    private static Class<?> loadClass(String className) throws ClassNotFoundException {
        return Class.forName(className, false, ConnectedHomeIpV161CompatibilityTest.class.getClassLoader());
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className, false, ConnectedHomeIpV161CompatibilityTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
