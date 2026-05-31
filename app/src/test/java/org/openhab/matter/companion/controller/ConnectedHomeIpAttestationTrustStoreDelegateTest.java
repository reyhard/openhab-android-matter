package org.openhab.matter.companion.controller;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public final class ConnectedHomeIpAttestationTrustStoreDelegateTest {
    @Test
    public void returnsPaaCertificateMatchingSkid() {
        byte[] certA = new byte[] {1, 2, 3};
        byte[] certB = new byte[] {9, 8, 7};
        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStore(Arrays.asList(certA, certB), Collections.emptyList());

        Object proxy = new ConnectedHomeIpAttestationTrustStoreDelegate(
                FakeAttestationTrustStoreDelegate.class,
                FakeDeviceAttestation.class,
                store)
                .proxy();

        byte[] result = ((FakeAttestationTrustStoreDelegate) proxy)
                .getProductAttestationAuthorityCert(new byte[] {7});

        assertArrayEquals(certB, result);
    }

    @Test
    public void returnsNullWhenSkidIsUnknown() {
        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStore(
                        Collections.singletonList(new byte[] {1, 2, 3}),
                        Collections.emptyList());

        Object proxy = new ConnectedHomeIpAttestationTrustStoreDelegate(
                FakeAttestationTrustStoreDelegate.class,
                FakeDeviceAttestation.class,
                store)
                .proxy();

        byte[] result = ((FakeAttestationTrustStoreDelegate) proxy)
                .getProductAttestationAuthorityCert(new byte[] {99});

        assertNull(result);
    }

    public interface FakeAttestationTrustStoreDelegate {
        byte[] getProductAttestationAuthorityCert(byte[] skid);
    }

    public static final class FakeDeviceAttestation {
        public static byte[] extractSkidFromPaaCert(byte[] cert) {
            return new byte[] {cert[cert.length - 1]};
        }
    }
}
