package org.openhab.matter.companion.controller;

import chip.devicecontroller.AttestationTrustStoreDelegate;
import chip.devicecontroller.DeviceAttestation;

import java.util.Arrays;

public final class ConnectedHomeIpConcreteAttestationTrustStoreDelegate implements AttestationTrustStoreDelegate {
    private final ConnectedHomeIpAttestationTrustStore store;

    public ConnectedHomeIpConcreteAttestationTrustStoreDelegate(ConnectedHomeIpAttestationTrustStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store is required");
        }
        this.store = store;
    }

    @Override
    public byte[] getProductAttestationAuthorityCert(byte[] skid) {
        if (skid == null) {
            return null;
        }
        for (byte[] certificate : store.paaCertificates()) {
            byte[] certificateSkid = DeviceAttestation.extractSkidFromPaaCert(certificate);
            if (Arrays.equals(certificateSkid, skid)) {
                return certificate;
            }
        }
        return null;
    }
}
