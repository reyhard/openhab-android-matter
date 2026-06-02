package org.openhab.matter.companion.controller;

import android.bluetooth.BluetoothGatt;

import org.junit.Test;
import org.openhab.matter.companion.domain.ThreadDataset;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class ConnectedHomeIpReflectionCommandFactoryTest {
    @Test
    public void buildsThreadCommissionParametersWithNetworkCredentials() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();

        Object params = factory.newThreadCommissionParameters(ThreadDataset.parse("hex:0E08FF1000000000"));

        assertArrayEquals(new byte[] {
                0x0E,
                0x08,
                (byte) 0xFF,
                0x10,
                0x00,
                0x00,
                0x00,
                0x00
        }, FakeNetworkCredentials.lastThreadCredentials.operationalDataset);
        assertSame(FakeCommissionParameters.BUILT, params);
        assertNull(FakeCommissionParameters.lastBuilder.csrNonce);
        assertEquals("network:8", FakeCommissionParameters.lastBuilder.networkCredentials);
        assertSame(FakeICDRegistrationInfo.DEFERRED, FakeCommissionParameters.lastBuilder.icdRegistrationInfo);
    }

    @Test
    public void buildsThreadCommissionParametersWithBuilderIcdFallbackWhenDeferredFactoryMissing() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = fakeFactory(FakeICDRegistrationInfoWithoutDeferredFactory.class);

        Object params = factory.newThreadCommissionParameters(ThreadDataset.parse("hex:0E08FF1000000000"));

        assertSame(FakeCommissionParameters.BUILT, params);
        assertSame(
                FakeICDRegistrationInfoWithoutDeferredFactory.BUILT,
                FakeCommissionParameters.lastBuilder.icdRegistrationInfo);
        assertSame(
                FakeICDRegistrationInfoWithoutDeferredFactory.lastBuilder,
                FakeICDRegistrationInfoWithoutDeferredFactory.Builder.lastBuiltBuilder);
    }

    @Test
    public void buildsStayActiveIcdRegistrationInfoAndUpdatesCommissioning() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
        FakeChipDeviceController controller = new FakeChipDeviceController();

        Object icdRegistrationInfo = factory.newIcdRegistrationInfoForStayActive(30000L);
        factory.invokeUpdateCommissioningIcdRegistrationInfo(controller, icdRegistrationInfo);

        assertSame(FakeICDRegistrationInfo.BUILT, icdRegistrationInfo);
        assertEquals(Long.valueOf(30000L), FakeICDRegistrationInfo.lastBuilder.stayActiveDurationMsec);
        assertSame(FakeICDRegistrationInfo.BUILT, controller.icdRegistrationInfo);
    }

    @Test
    public void locatesConnectedHomeIpControllerCommandMethods() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();

        Method pair = factory.pairDeviceThroughBleMethod();
        Method ocw = factory.openPairingWindowWithPinCallbackMethod();

        assertEquals("pairDeviceThroughBLE", pair.getName());
        assertEquals(BluetoothGatt.class, pair.getParameterTypes()[0]);
        assertEquals(FakeCommissionParameters.class, pair.getParameterTypes()[4]);
        assertEquals("openPairingWindowWithPINCallback", ocw.getName());
        assertEquals(FakeOpenCommissioningCallback.class, ocw.getParameterTypes()[5]);
    }

    @Test
    public void invokesPairDeviceThroughBle() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
        FakeChipDeviceController controller = new FakeChipDeviceController();
        Object params = FakeCommissionParameters.BUILT;

        factory.invokePairDeviceThroughBle(controller, null, 42, 987654321L, 20202021L, params);

        assertEquals(42, controller.connId);
        assertEquals(987654321L, controller.deviceId);
        assertEquals(20202021L, controller.setupPin);
        assertSame(params, controller.params);
    }

    @Test
    public void invokesUnpair() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
        FakeChipDeviceController controller = new FakeChipDeviceController();

        factory.invokeUnpair(controller, 0x165BC267A7E344D0L);

        assertEquals(0x165BC267A7E344D0L, controller.unpairNodeId);
    }

    @Test
    public void invokesOpenPairingWindowAndReturnsManualCodeFromCallback() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
        FakeChipDeviceController controller = new FakeChipDeviceController();
        ConnectedHomeIpOpenCommissioningWindowCallback callback =
                factory.newOpenCommissioningWindowCallback("controller-state");

        boolean started = factory.invokeOpenPairingWindowWithPinCallback(
                controller,
                1234L,
                new ConnectedHomeIpOpenCommissioningWindowRequest(987654321L, 300, 1000L, 3840, "controller-state"),
                null,
                callback.proxy());

        MatterOpenCommissioningWindowResult result = callback.awaitResult(1000);

        assertTrue(started);
        assertEquals(1234L, controller.devicePtr);
        assertEquals(300, controller.duration);
        assertEquals(1000L, controller.iteration);
        assertEquals(3840, controller.discriminator);
        assertEquals("3497-0112-332", result.temporaryCode());
        assertEquals("MT:TEST", result.qrCode());
        assertEquals("controller-state", result.controllerState());
    }

    @Test
    public void openCommissioningWindowCallbackReportsError() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
        FakeChipDeviceController controller = new FakeChipDeviceController();
        controller.openCommissioningWindowError = true;
        ConnectedHomeIpOpenCommissioningWindowCallback callback =
                factory.newOpenCommissioningWindowCallback("controller-state");

        factory.invokeOpenPairingWindowWithPinCallback(
                controller,
                1234L,
                new ConnectedHomeIpOpenCommissioningWindowRequest(987654321L, 300, 1000L, 3840, "controller-state"),
                null,
                callback.proxy());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> callback.awaitResult(1000));

        assertTrue(exception.getMessage().contains("55"));
        assertTrue(exception.getMessage().contains("987654321"));
    }

    @Test
    public void openCommissioningWindowCallbackReportsBlankSuccessCodesAsError() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
        FakeChipDeviceController controller = new FakeChipDeviceController();
        controller.blankOpenCommissioningWindowSuccess = true;
        ConnectedHomeIpOpenCommissioningWindowCallback callback =
                factory.newOpenCommissioningWindowCallback("controller-state");

        factory.invokeOpenPairingWindowWithPinCallback(
                controller,
                1234L,
                new ConnectedHomeIpOpenCommissioningWindowRequest(987654321L, 300, 1000L, 3840, "controller-state"),
                null,
                callback.proxy());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> callback.awaitResult(1000));

        assertTrue(exception.getMessage().contains("blank"));
    }

    @Test
    public void invokesSetAttestationTrustStoreDelegateWithCdTrustKeys() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
        FakeChipDeviceController controller = new FakeChipDeviceController();

        factory.invokeSetAttestationTrustStoreDelegate(
                controller,
                new FakeAttestationTrustStoreDelegate() {
                },
                Arrays.asList(new byte[] {1, 2, 3}));

        assertTrue(controller.attestationTrustStoreDelegateSet);
        assertEquals(1, controller.cdTrustKeys.size());
    }

    private static ConnectedHomeIpReflectionCommandFactory fakeFactory() {
        return fakeFactory(FakeICDRegistrationInfo.class);
    }

    private static ConnectedHomeIpReflectionCommandFactory fakeFactory(Class<?> icdRegistrationInfoClass) {
        return new ConnectedHomeIpReflectionCommandFactory(
                FakeNetworkCredentials.class,
                FakeNetworkCredentials.ThreadCredentials.class,
                FakeCommissionParameters.Builder.class,
                FakeChipDeviceController.class,
                FakeOpenCommissioningCallback.class,
                null,
                null,
                null,
                icdRegistrationInfoClass,
                FakeAttestationTrustStoreDelegate.class,
                FakeDeviceAttestation.class);
    }

    public static final class FakeNetworkCredentials {
        private static ThreadCredentials lastThreadCredentials;

        public static Object forThread(ThreadCredentials threadCredentials) {
            lastThreadCredentials = threadCredentials;
            return "network:" + threadCredentials.operationalDataset.length;
        }

        public static final class ThreadCredentials {
            private final byte[] operationalDataset;

            public ThreadCredentials(byte[] operationalDataset) {
                this.operationalDataset = operationalDataset;
            }
        }
    }

    public static final class FakeCommissionParameters {
        private static final FakeCommissionParameters BUILT = new FakeCommissionParameters();
        private static Builder lastBuilder;

        public static final class Builder {
            private Object csrNonce = "unset";
            private Object networkCredentials;
            private Object icdRegistrationInfo = "unset";

            public Builder() {
                lastBuilder = this;
            }

            public Builder setCsrNonce(byte[] csrNonce) {
                this.csrNonce = csrNonce;
                return this;
            }

            public Builder setNetworkCredentials(Object networkCredentials) {
                this.networkCredentials = networkCredentials;
                return this;
            }

            public Builder setICDRegistrationInfo(Object icdRegistrationInfo) {
                this.icdRegistrationInfo = icdRegistrationInfo;
                return this;
            }

            public FakeCommissionParameters build() {
                return BUILT;
            }
        }
    }

    public static final class FakeICDRegistrationInfo {
        static final FakeICDRegistrationInfo DEFERRED = new FakeICDRegistrationInfo();
        static final FakeICDRegistrationInfo BUILT = new FakeICDRegistrationInfo();
        static Builder lastBuilder;

        public static FakeICDRegistrationInfo createForDeferredConfiguration() {
            return DEFERRED;
        }

        public static Builder newBuilder() {
            lastBuilder = new Builder();
            return lastBuilder;
        }

        public static final class Builder {
            Long stayActiveDurationMsec;

            public Builder setICDStayActiveDurationMsec(Long stayActiveDurationMsec) {
                this.stayActiveDurationMsec = stayActiveDurationMsec;
                return this;
            }

            public FakeICDRegistrationInfo build() {
                return BUILT;
            }
        }
    }

    public static final class FakeICDRegistrationInfoWithoutDeferredFactory {
        static final FakeICDRegistrationInfoWithoutDeferredFactory BUILT =
                new FakeICDRegistrationInfoWithoutDeferredFactory();
        static Builder lastBuilder;

        public static Builder newBuilder() {
            lastBuilder = new Builder();
            return lastBuilder;
        }

        public static final class Builder {
            static Builder lastBuiltBuilder;

            public FakeICDRegistrationInfoWithoutDeferredFactory build() {
                lastBuiltBuilder = this;
                return BUILT;
            }
        }
    }

    public static final class FakeChipDeviceController {
        private int connId;
        private long deviceId;
        private long setupPin;
        private Object params;
        private long devicePtr;
        private int duration;
        private long iteration;
        private int discriminator;
        private boolean openCommissioningWindowError;
        private boolean blankOpenCommissioningWindowSuccess;
        private FakeICDRegistrationInfo icdRegistrationInfo;
        private boolean attestationTrustStoreDelegateSet;
        private List<byte[]> cdTrustKeys;
        private long unpairNodeId = -1L;

        public void pairDeviceThroughBLE(
                BluetoothGatt bleServer,
                int connId,
                long deviceId,
                long setupPincode,
                FakeCommissionParameters params) {
            this.connId = connId;
            this.deviceId = deviceId;
            this.setupPin = setupPincode;
            this.params = params;
        }

        public boolean openPairingWindowWithPINCallback(
                long devicePtr,
                int duration,
                long iteration,
                int discriminator,
                Long setupPinCode,
                FakeOpenCommissioningCallback callback) {
            this.devicePtr = devicePtr;
            this.duration = duration;
            this.iteration = iteration;
            this.discriminator = discriminator;
            if (openCommissioningWindowError) {
                callback.onError(55, 987654321L);
            } else if (blankOpenCommissioningWindowSuccess) {
                callback.onSuccess(987654321L, "", "");
            } else {
                callback.onSuccess(987654321L, "3497-0112-332", "MT:TEST");
            }
            return true;
        }

        public void updateCommissioningICDRegistrationInfo(FakeICDRegistrationInfo icdRegistrationInfo) {
            this.icdRegistrationInfo = icdRegistrationInfo;
        }

        public void unpairDevice(long nodeId) {
            this.unpairNodeId = nodeId;
        }

        public void setAttestationTrustStoreDelegate(
                FakeAttestationTrustStoreDelegate delegate,
                List<byte[]> cdTrustKeys) {
            this.attestationTrustStoreDelegateSet = delegate != null;
            this.cdTrustKeys = cdTrustKeys;
        }
    }

    public interface FakeOpenCommissioningCallback {
        void onError(int status, long deviceId);

        void onSuccess(long deviceId, String manualPairingCode, String qrCode);
    }

    public interface FakeAttestationTrustStoreDelegate {
    }

    public static final class FakeDeviceAttestation {
        public static byte[] extractSkidFromPaaCert(byte[] cert) {
            return cert;
        }
    }
}
