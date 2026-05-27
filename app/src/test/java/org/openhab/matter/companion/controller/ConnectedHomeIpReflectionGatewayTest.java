package org.openhab.matter.companion.controller;

import android.bluetooth.BluetoothGatt;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class ConnectedHomeIpReflectionGatewayTest {
    @Test
    public void commissionBleThreadInvokesPairingCommandAndWaitsForCompletion() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        CapturingBleConnectionProvider bleProvider = new CapturingBleConnectionProvider();
        CapturingCommissioningMonitor monitor = new CapturingCommissioningMonitor();
        CapturingAttestationHandler attestationHandler = new CapturingAttestationHandler();
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> controller,
                bleProvider,
                () -> 987654321L,
                monitor,
                attestationHandler,
                unusedPointerProvider(),
                fakeCommandFactory(),
                1000L);

        MatterCommissioningResult result = gateway.commissionBleThread(new ConnectedHomeIpCommissioningRequest(
                "0E080000000000010000",
                20202021L,
                3840,
                true,
                "controller-state"));

        assertEquals(3840, bleProvider.discriminator);
        assertTrue(bleProvider.connection.closed);
        assertEquals(42, controller.connId);
        assertEquals(987654321L, controller.deviceId);
        assertEquals(20202021L, controller.setupPin);
        assertSame(FakeCommissionParameters.BUILT, controller.params);
        assertArrayEquals(new byte[] {0x0E, 0x08, 0, 0, 0, 0, 0, 1, 0, 0}, FakeNetworkCredentials.lastDataset);
        assertSame(controller, attestationHandler.controller);
        assertEquals(987654321L, attestationHandler.nodeId);
        assertTrue(attestationHandler.attestationBypassEnabled);
        assertEquals(987654321L, monitor.nodeId);
        assertEquals("controller-state", monitor.controllerState);
        assertEquals(987654321L, result.nodeId());
        assertEquals("commissioned-state", result.controllerState());
    }

    @Test
    public void commissionBleThreadPreparesMonitorBeforePairingCommand() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        CapturingCommissioningMonitor monitor = new CapturingCommissioningMonitor();
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> controller,
                new CapturingBleConnectionProvider(),
                () -> 987654321L,
                monitor,
                unusedAttestationHandler(),
                unusedPointerProvider(),
                fakeCommandFactory(),
                1000L);

        gateway.commissionBleThread(new ConnectedHomeIpCommissioningRequest(
                "0E080000000000010000",
                20202021L,
                3840,
                false,
                "controller-state"));

        assertSame(controller, monitor.preparedController);
        assertTrue(controller.pairedAfterMonitorPrepared);
    }

    @Test
    public void commissionBleThreadClearsStaleBleConnectionBeforePairingCommand() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        controller.staleBleConnection = true;
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> controller,
                new CapturingBleConnectionProvider(),
                () -> 987654321L,
                new CapturingCommissioningMonitor(),
                unusedAttestationHandler(),
                unusedPointerProvider(),
                fakeCommandFactory(),
                1000L);

        gateway.commissionBleThread(new ConnectedHomeIpCommissioningRequest(
                "0E080000000000010000",
                20202021L,
                3840,
                false,
                "controller-state"));

        assertEquals(1, controller.closeCalls);
        assertFalse(controller.staleBleConnection);
        assertEquals(42, controller.connId);
    }

    @Test
    public void commissionBleThreadCanUseReflectionMonitorToInstallCompletionListenerBeforePairing() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        controller.requireCompletionListenerBeforePairing = true;
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> controller,
                new CapturingBleConnectionProvider(),
                () -> 987654321L,
                new ConnectedHomeIpReflectionCommissioningMonitor(fakeCommandFactory(), 1000L),
                unusedAttestationHandler(),
                unusedPointerProvider(),
                fakeCommandFactory(),
                1000L);

        MatterCommissioningResult result = gateway.commissionBleThread(new ConnectedHomeIpCommissioningRequest(
                "0E080000000000010000",
                20202021L,
                3840,
                false,
                "controller-state"));

        assertEquals(987654321L, result.nodeId());
        assertSame(controller.completionListenerAtPairing, controller.completionListener);
    }

    @Test
    public void commissionBleThreadDoesNotConnectBleWhenMonitorPreparationFails() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> controller,
                unusedBleProvider(),
                () -> 987654321L,
                new ThrowingCommissioningMonitor(),
                unusedAttestationHandler(),
                unusedPointerProvider(),
                fakeCommandFactory(),
                1000L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> gateway.commissionBleThread(
                new ConnectedHomeIpCommissioningRequest(
                        "0E080000000000010000",
                        20202021L,
                        3840,
                        false,
                        "controller-state")));

        assertTrue(exception.getMessage().contains("prepare failed"));
    }

    @Test
    public void commissionBleThreadClosesBleConnectionWhenPairingCommandFails() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        controller.failPairing = true;
        CapturingBleConnectionProvider bleProvider = new CapturingBleConnectionProvider();
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> controller,
                bleProvider,
                () -> 987654321L,
                new CapturingCommissioningMonitor(),
                unusedAttestationHandler(),
                unusedPointerProvider(),
                fakeCommandFactory(),
                1000L);

        assertThrows(Exception.class, () -> gateway.commissionBleThread(new ConnectedHomeIpCommissioningRequest(
                "0E080000000000010000",
                20202021L,
                3840,
                false,
                "controller-state")));

        assertTrue(bleProvider.connection.closed);
    }

    @Test
    public void openCommissioningWindowInvokesCallbackCommandAndReleasesDevicePointer() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        CapturingDevicePointerProvider pointerProvider = new CapturingDevicePointerProvider(1234L);
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> controller,
                unusedBleProvider(),
                () -> 1L,
                new CapturingCommissioningMonitor(),
                unusedAttestationHandler(),
                pointerProvider,
                fakeCommandFactory(),
                1000L);

        MatterOpenCommissioningWindowResult result = gateway.openCommissioningWindow(
                new ConnectedHomeIpOpenCommissioningWindowRequest(
                        987654321L,
                        300,
                        1000L,
                        3840,
                        "controller-state"));

        assertEquals(987654321L, pointerProvider.nodeId);
        assertSame(controller, pointerProvider.controller);
        assertTrue(pointerProvider.pointer.released);
        assertEquals(1234L, controller.devicePtr);
        assertEquals(300, controller.duration);
        assertEquals(1000L, controller.iteration);
        assertEquals(3840, controller.discriminator);
        assertEquals("3497-0112-332", result.temporaryCode());
        assertEquals("controller-state", result.controllerState());
    }

    @Test
    public void checkFabricRestoreUsesExistingControllerAndReleasesDevicePointer() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        CapturingDevicePointerProvider pointerProvider = new CapturingDevicePointerProvider(1234L);
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> controller,
                unusedBleProvider(),
                () -> 1L,
                new CapturingCommissioningMonitor(),
                unusedAttestationHandler(),
                pointerProvider,
                fakeCommandFactory(),
                1000L);

        ConnectedHomeIpFabricRestoreStatus status = gateway.checkFabricRestore(987654321L);

        assertTrue(status.checked());
        assertTrue(status.ready());
        assertEquals(987654321L, status.nodeId());
        assertSame(controller, pointerProvider.controller);
        assertEquals(987654321L, pointerProvider.nodeId);
        assertTrue(pointerProvider.pointer.released);
    }

    @Test
    public void checkRuntimePreflightBuildsControllerAndChecksBleManagerCallback() {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        CapturingControllerProvider controllerProvider = new CapturingControllerProvider(controller);
        CapturingRuntimePreflightBleProvider bleProvider = new CapturingRuntimePreflightBleProvider();
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                controllerProvider,
                bleProvider,
                () -> 1L,
                new CapturingCommissioningMonitor(),
                unusedAttestationHandler(),
                unusedPointerProvider(),
                fakeCommandFactory(),
                1000L);

        ConnectedHomeIpRuntimePreflightStatus status = gateway.checkRuntimePreflight();

        assertTrue(status.ready());
        assertTrue(status.message().contains("controller "));
        assertTrue(status.message().contains("BLE manager initialized"));
        assertSame(controller, controllerProvider.controller);
        assertTrue(bleProvider.checked);
    }

    @Test
    public void checkRuntimePreflightReportsControllerConstructionFailure() {
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> {
                    throw new IllegalStateException("controller boom");
                },
                unusedBleProvider(),
                () -> 1L,
                new CapturingCommissioningMonitor(),
                unusedAttestationHandler(),
                unusedPointerProvider(),
                fakeCommandFactory(),
                1000L);

        ConnectedHomeIpRuntimePreflightStatus status = gateway.checkRuntimePreflight();

        assertFalse(status.ready());
        assertTrue(status.message().contains("controller boom"));
    }

    @Test
    public void openCommissioningWindowReleasesDevicePointerWhenCallbackFails() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        controller.blankOpenCommissioningWindowSuccess = true;
        CapturingDevicePointerProvider pointerProvider = new CapturingDevicePointerProvider(1234L);
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> controller,
                unusedBleProvider(),
                () -> 1L,
                new CapturingCommissioningMonitor(),
                unusedAttestationHandler(),
                pointerProvider,
                fakeCommandFactory(),
                1000L);

        assertThrows(IllegalStateException.class, () -> gateway.openCommissioningWindow(
                new ConnectedHomeIpOpenCommissioningWindowRequest(
                        987654321L,
                        300,
                        1000L,
                        3840,
                        "controller-state")));

        assertTrue(pointerProvider.pointer.released);
    }

    @Test
    public void openCommissioningWindowRejectsFalseCommandStart() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        controller.openPairingWindowStarted = false;
        CapturingDevicePointerProvider pointerProvider = new CapturingDevicePointerProvider(1234L);
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> controller,
                unusedBleProvider(),
                () -> 1L,
                new CapturingCommissioningMonitor(),
                unusedAttestationHandler(),
                pointerProvider,
                fakeCommandFactory(),
                1000L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> gateway.openCommissioningWindow(
                new ConnectedHomeIpOpenCommissioningWindowRequest(
                        987654321L,
                        300,
                        1000L,
                        3840,
                        "controller-state")));

        assertTrue(exception.getMessage().contains("did not start"));
        assertTrue(pointerProvider.pointer.released);
    }

    @Test
    public void openCommissioningWindowReleasesDevicePointerWhenInvocationThrows() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        controller.failOpenPairingWindowInvocation = true;
        CapturingDevicePointerProvider pointerProvider = new CapturingDevicePointerProvider(1234L);
        ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
                () -> controller,
                unusedBleProvider(),
                () -> 1L,
                new CapturingCommissioningMonitor(),
                unusedAttestationHandler(),
                pointerProvider,
                fakeCommandFactory(),
                1000L);

        assertThrows(Exception.class, () -> gateway.openCommissioningWindow(
                new ConnectedHomeIpOpenCommissioningWindowRequest(
                        987654321L,
                        300,
                        1000L,
                        3840,
                        "controller-state")));

        assertTrue(pointerProvider.pointer.released);
    }

    private static ConnectedHomeIpReflectionCommandFactory fakeCommandFactory() {
        return new ConnectedHomeIpReflectionCommandFactory(
                FakeNetworkCredentials.class,
                FakeNetworkCredentials.ThreadCredentials.class,
                FakeCommissionParameters.Builder.class,
                FakeChipDeviceController.class,
                FakeOpenCommissioningCallback.class);
    }

    private static ConnectedHomeIpBleConnectionProvider unusedBleProvider() {
        return discriminator -> {
            throw new AssertionError("BLE provider should not be called");
        };
    }

    private static ConnectedHomeIpDevicePointerProvider unusedPointerProvider() {
        return (controller, nodeId) -> {
            throw new AssertionError("device pointer provider should not be called");
        };
    }

    private static ConnectedHomeIpAttestationHandler unusedAttestationHandler() {
        return (controller, nodeId, attestationBypassEnabled) -> { };
    }

    public static final class FakeNetworkCredentials {
        private static byte[] lastDataset;

        public static Object forThread(ThreadCredentials threadCredentials) {
            lastDataset = threadCredentials.operationalDataset;
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

        public static final class Builder {
            public Builder setCsrNonce(byte[] csrNonce) {
                return this;
            }

            public Builder setNetworkCredentials(Object networkCredentials) {
                return this;
            }

            public Builder setICDRegistrationInfo(Object icdRegistrationInfo) {
                return this;
            }

            public FakeCommissionParameters build() {
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
        private boolean failPairing;
        private boolean blankOpenCommissioningWindowSuccess;
        private boolean openPairingWindowStarted = true;
        private boolean failOpenPairingWindowInvocation;
        private boolean monitorPrepared;
        private boolean pairedAfterMonitorPrepared;
        private boolean requireCompletionListenerBeforePairing;
        private boolean staleBleConnection;
        private int closeCalls;
        private CompletionListener completionListener;
        private CompletionListener completionListenerAtPairing;

        public void setCompletionListener(CompletionListener listener) {
            completionListener = listener;
        }

        public void close() {
            closeCalls++;
            staleBleConnection = false;
        }

        public void pairDeviceThroughBLE(
                BluetoothGatt bleServer,
                int connId,
                long deviceId,
                long setupPincode,
                FakeCommissionParameters params) {
            completionListenerAtPairing = completionListener;
            if (requireCompletionListenerBeforePairing && completionListener == null) {
                throw new IllegalStateException("completion listener missing before pairing");
            }
            if (failPairing) {
                throw new IllegalStateException("pair failed");
            }
            if (staleBleConnection) {
                throw new IllegalStateException("Bluetooth connection already in use.");
            }
            this.connId = connId;
            this.deviceId = deviceId;
            this.setupPin = setupPincode;
            this.params = params;
            pairedAfterMonitorPrepared = monitorPrepared;
            if (completionListener != null) {
                completionListener.onCommissioningComplete(deviceId, 0L);
            }
        }

        public boolean openPairingWindowWithPINCallback(
                long devicePtr,
                int duration,
                long iteration,
                int discriminator,
                Long setupPinCode,
                FakeOpenCommissioningCallback callback) {
            if (failOpenPairingWindowInvocation) {
                throw new IllegalStateException("OCW invocation failed");
            }
            if (!openPairingWindowStarted) {
                return false;
            }
            this.devicePtr = devicePtr;
            this.duration = duration;
            this.iteration = iteration;
            this.discriminator = discriminator;
            if (blankOpenCommissioningWindowSuccess) {
                callback.onSuccess(987654321L, "", "");
            } else {
                callback.onSuccess(987654321L, "3497-0112-332", "MT:TEST");
            }
            return true;
        }

        public interface CompletionListener {
            void onCommissioningComplete(long nodeId, long errorCode);
        }
    }

    public interface FakeOpenCommissioningCallback {
        void onError(int status, long deviceId);

        void onSuccess(long deviceId, String manualPairingCode, String qrCode);
    }

    private static final class CapturingBleConnectionProvider implements ConnectedHomeIpBleConnectionProvider {
        private final CapturingBleConnection connection = new CapturingBleConnection();
        private int discriminator;

        @Override
        public ConnectedHomeIpBleConnection connect(int discriminator) {
            this.discriminator = discriminator;
            return connection;
        }
    }

    private static final class CapturingRuntimePreflightBleProvider
            implements ConnectedHomeIpBleConnectionProvider, ConnectedHomeIpRuntimePreflightChecker {
        private boolean checked;

        @Override
        public ConnectedHomeIpBleConnection connect(int discriminator) {
            throw new AssertionError("runtime preflight must not connect to BLE devices");
        }

        @Override
        public ConnectedHomeIpRuntimePreflightStatus checkRuntimePreflight() {
            checked = true;
            return new ConnectedHomeIpRuntimePreflightStatus(true, "ble-ok");
        }
    }

    private static final class CapturingControllerProvider implements ConnectedHomeIpControllerProvider {
        private final Object controller;

        private CapturingControllerProvider(Object controller) {
            this.controller = controller;
        }

        @Override
        public Object controller() {
            return controller;
        }
    }

    private static final class CapturingBleConnection extends ConnectedHomeIpBleConnection {
        private boolean closed;

        private CapturingBleConnection() {
            super(null, 42, () -> { });
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class CapturingAttestationHandler implements ConnectedHomeIpAttestationHandler {
        private Object controller;
        private long nodeId;
        private boolean attestationBypassEnabled;

        @Override
        public void prepareForCommissioning(Object controller, long nodeId, boolean attestationBypassEnabled) {
            this.controller = controller;
            this.nodeId = nodeId;
            this.attestationBypassEnabled = attestationBypassEnabled;
        }
    }

    private static final class CapturingCommissioningMonitor implements ConnectedHomeIpCommissioningMonitor {
        private Object preparedController;
        private long nodeId;
        private String controllerState;

        @Override
        public void prepare(Object controller) {
            preparedController = controller;
            ((FakeChipDeviceController) controller).monitorPrepared = true;
        }

        @Override
        public MatterCommissioningResult awaitCommissioned(long nodeId, String controllerState) {
            this.nodeId = nodeId;
            this.controllerState = controllerState;
            return new MatterCommissioningResult(nodeId, "commissioned-state");
        }
    }

    private static final class ThrowingCommissioningMonitor implements ConnectedHomeIpCommissioningMonitor {
        @Override
        public void prepare(Object controller) {
            throw new IllegalStateException("prepare failed");
        }

        @Override
        public MatterCommissioningResult awaitCommissioned(long nodeId, String controllerState) {
            throw new AssertionError("await should not be called");
        }
    }

    private static final class CapturingDevicePointerProvider implements ConnectedHomeIpDevicePointerProvider {
        private final CapturingDevicePointer pointer;
        private Object controller;
        private long nodeId;

        private CapturingDevicePointerProvider(long devicePtr) {
            pointer = new CapturingDevicePointer(devicePtr);
        }

        @Override
        public ConnectedHomeIpDevicePointer acquire(Object controller, long nodeId) {
            this.controller = controller;
            this.nodeId = nodeId;
            return pointer;
        }
    }

    private static final class CapturingDevicePointer extends ConnectedHomeIpDevicePointer {
        private boolean released;

        private CapturingDevicePointer(long value) {
            super(value, () -> { });
        }

        @Override
        public void close() {
            released = true;
        }
    }
}
