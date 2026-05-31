package org.openhab.matter.companion.controller;

import org.junit.Test;

import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ConnectedHomeIpReflectionAttestationHandlerTest {
    @Test
    public void preparesTrustStoreBeforeDeviceAttestationDelegate() throws Exception {
        ConnectedHomeIpReflectionCommandFactory commandFactory = new ConnectedHomeIpReflectionCommandFactory(
                Object.class,
                Object.class,
                Object.class,
                FakeChipDeviceController.class,
                Object.class,
                Object.class,
                FakeDeviceAttestationDelegate.class,
                Object.class,
                null,
                FakeAttestationTrustStoreDelegate.class,
                FakeDeviceAttestation.class);
        FakeChipDeviceController controller = new FakeChipDeviceController();
        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStore(Arrays.asList(new byte[] {1, 2, 3}), Arrays.asList());
        ConnectedHomeIpReflectionAttestationHandler handler =
                new ConnectedHomeIpReflectionAttestationHandler(commandFactory, 120, store);

        handler.prepareForCommissioning(controller, 987654321L, false);

        assertEquals("setTrustStore,setDeviceDelegate", controller.sequence.toString());
        assertEquals(120, controller.failSafeExpiryTimeoutSeconds);
        assertTrue(controller.attestationDelegateConfigured);
    }

    public static final class FakeChipDeviceController {
        private final StringBuilder sequence = new StringBuilder();
        private int failSafeExpiryTimeoutSeconds;
        private boolean attestationDelegateConfigured;
        private Object trustStoreDelegate;

        public void setAttestationTrustStoreDelegate(
                FakeAttestationTrustStoreDelegate delegate,
                java.util.List<byte[]> cdTrustKeys) {
            trustStoreDelegate = delegate;
            append("setTrustStore");
        }

        public void setDeviceAttestationDelegate(
                int failSafeExpiryTimeoutSeconds,
                FakeDeviceAttestationDelegate delegate) {
            this.failSafeExpiryTimeoutSeconds = failSafeExpiryTimeoutSeconds;
            this.attestationDelegateConfigured = delegate != null;
            append("setDeviceDelegate");
        }

        private void append(String step) {
            if (sequence.length() > 0) {
                sequence.append(',');
            }
            sequence.append(step);
        }
    }

    public interface FakeAttestationTrustStoreDelegate {
        byte[] getProductAttestationAuthorityCert(byte[] skid);
    }

    public interface FakeDeviceAttestationDelegate {
        void onDeviceAttestationCompleted(long devicePtr, Object attestationInfo, long errorCode);
    }

    public static final class FakeDeviceAttestation {
        public static byte[] extractSkidFromPaaCert(byte[] cert) {
            return new byte[] {cert[cert.length - 1]};
        }
    }
}
