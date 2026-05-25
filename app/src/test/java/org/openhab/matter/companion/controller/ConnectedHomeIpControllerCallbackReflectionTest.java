package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class ConnectedHomeIpControllerCallbackReflectionTest {
    @Test
    public void commissioningCompletionListenerReturnsSuccessfulCommissioningResult() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
        FakeChipDeviceController controller = new FakeChipDeviceController();
        ConnectedHomeIpCommissioningCompletionListener listener = factory.newCommissioningCompletionListener();

        factory.invokeSetCompletionListener(controller, listener.proxy());
        controller.completionListener.onCommissioningComplete(987654321L, 0L);

        MatterCommissioningResult result = listener.awaitCommissioned(987654321L, "controller-state");

        assertEquals(987654321L, result.nodeId());
        assertEquals("controller-state", result.controllerState());
    }

    @Test
    public void commissioningCompletionListenerReportsCommissioningError() throws Exception {
        ConnectedHomeIpCommissioningCompletionListener listener = fakeFactory().newCommissioningCompletionListener();

        ((FakeCompletionListener) listener.proxy()).onCommissioningComplete(987654321L, 55L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> listener.awaitCommissioned(987654321L, "controller-state"));

        assertTrue(exception.getMessage().contains("55"));
        assertTrue(exception.getMessage().contains("987654321"));
    }

    @Test
    public void commissioningCompletionListenerReportsPairingErrorBeforeCommissioningComplete() throws Exception {
        ConnectedHomeIpCommissioningCompletionListener listener = fakeFactory().newCommissioningCompletionListener();

        ((FakeCompletionListener) listener.proxy()).onPairingComplete(77L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> listener.awaitCommissioned(987654321L, "controller-state"));

        assertTrue(exception.getMessage().contains("77"));
    }

    @Test
    public void reflectionAttestationHandlerRegistersDelegateAndContinuesWithBypassFlag() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        ConnectedHomeIpReflectionAttestationHandler handler = new ConnectedHomeIpReflectionAttestationHandler(
                fakeFactory(),
                120);

        handler.prepareForCommissioning(controller, 987654321L, true);
        controller.attestationDelegate.onDeviceAttestationCompleted(1234L, new Object(), 42L);

        assertEquals(120, controller.attestationFailSafeExpiryTimeoutSecs);
        assertEquals(1234L, controller.continueCommissioningDevicePtr);
        assertTrue(controller.continueCommissioningIgnoreAttestationFailure);
    }

    @Test
    public void reflectionDevicePointerProviderReturnsPointerAndReleasesThroughController() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        ConnectedHomeIpReflectionDevicePointerProvider provider = new ConnectedHomeIpReflectionDevicePointerProvider(
                fakeFactory(),
                1000L);

        ConnectedHomeIpDevicePointer pointer = provider.acquire(controller, 987654321L);
        pointer.close();

        assertEquals(987654321L, controller.connectedDeviceNodeId);
        assertEquals(1234L, pointer.value());
        assertEquals(1234L, controller.releasedDevicePtr);
    }

    @Test
    public void reflectionDevicePointerProviderReportsConnectionFailure() {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        controller.connectedDeviceFailure = true;
        ConnectedHomeIpReflectionDevicePointerProvider provider = new ConnectedHomeIpReflectionDevicePointerProvider(
                fakeFactory(),
                1000L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> provider.acquire(controller, 987654321L));

        assertTrue(exception.getMessage().contains("987654321"));
    }

    @Test
    public void reflectionDevicePointerProviderReleasesLateSuccessAfterTimeout() throws Exception {
        FakeChipDeviceController controller = new FakeChipDeviceController();
        controller.connectedDeviceDelayed = true;
        ConnectedHomeIpReflectionDevicePointerProvider provider = new ConnectedHomeIpReflectionDevicePointerProvider(
                fakeFactory(),
                1L);

        assertThrows(
                IllegalStateException.class,
                () -> provider.acquire(controller, 987654321L));

        controller.connectedDeviceCallback.onDeviceConnected(1234L);

        assertEquals(1234L, controller.releasedDevicePtr);
    }

    private static ConnectedHomeIpReflectionCommandFactory fakeFactory() {
        return new ConnectedHomeIpReflectionCommandFactory(
                ConnectedHomeIpReflectionCommandFactoryTest.FakeNetworkCredentials.class,
                ConnectedHomeIpReflectionCommandFactoryTest.FakeNetworkCredentials.ThreadCredentials.class,
                ConnectedHomeIpReflectionCommandFactoryTest.FakeCommissionParameters.Builder.class,
                FakeChipDeviceController.class,
                ConnectedHomeIpReflectionCommandFactoryTest.FakeOpenCommissioningCallback.class,
                FakeCompletionListener.class,
                FakeDeviceAttestationDelegate.class,
                FakeGetConnectedDeviceCallback.class);
    }

    public static final class FakeChipDeviceController {
        private FakeCompletionListener completionListener;
        private int attestationFailSafeExpiryTimeoutSecs;
        private FakeDeviceAttestationDelegate attestationDelegate;
        private long continueCommissioningDevicePtr;
        private boolean continueCommissioningIgnoreAttestationFailure;
        private long connectedDeviceNodeId;
        private boolean connectedDeviceFailure;
        private boolean connectedDeviceDelayed;
        private FakeGetConnectedDeviceCallback connectedDeviceCallback;
        private long releasedDevicePtr;

        public void setCompletionListener(FakeCompletionListener listener) {
            completionListener = listener;
        }

        public void setDeviceAttestationDelegate(
                int failSafeExpiryTimeoutSecs,
                FakeDeviceAttestationDelegate delegate) {
            this.attestationFailSafeExpiryTimeoutSecs = failSafeExpiryTimeoutSecs;
            attestationDelegate = delegate;
        }

        public void continueCommissioning(long devicePtr, boolean ignoreAttestationFailure) {
            continueCommissioningDevicePtr = devicePtr;
            continueCommissioningIgnoreAttestationFailure = ignoreAttestationFailure;
        }

        public void getConnectedDevicePointer(long nodeId, FakeGetConnectedDeviceCallback callback) {
            connectedDeviceNodeId = nodeId;
            connectedDeviceCallback = callback;
            if (connectedDeviceDelayed) {
                return;
            }
            if (connectedDeviceFailure) {
                callback.onConnectionFailure(nodeId, new IllegalStateException("connect failed"));
            } else {
                callback.onDeviceConnected(1234L);
            }
        }

        public void releaseConnectedDevicePointer(long devicePtr) {
            releasedDevicePtr = devicePtr;
        }
    }

    public interface FakeCompletionListener {
        void onConnectDeviceComplete();

        void onStatusUpdate(int status);

        void onPairingComplete(long errorCode);

        void onCommissioningComplete(long nodeId, long errorCode);

        void onCommissioningStatusUpdate(long nodeId, String stage, long errorCode);

        void onCommissioningStageStart(long nodeId, String stage);

        void onNotifyChipConnectionClosed();

        void onCloseBleComplete();

        void onError(Throwable error);
    }

    public interface FakeDeviceAttestationDelegate {
        void onDeviceAttestationCompleted(long devicePtr, Object attestationInfo, long errorCode);
    }

    public interface FakeGetConnectedDeviceCallback {
        void onDeviceConnected(long devicePointer);

        void onConnectionFailure(long nodeId, Exception error);
    }
}
