package org.openhab.matter.companion.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConnectedHomeIpAttestationTrustStore {
    private final List<byte[]> paaCertificates;
    private final List<byte[]> cdTrustKeys;

    public ConnectedHomeIpAttestationTrustStore(List<byte[]> paaCertificates, List<byte[]> cdTrustKeys) {
        this.paaCertificates = copy(paaCertificates);
        this.cdTrustKeys = copy(cdTrustKeys);
    }

    public List<byte[]> paaCertificates() {
        return paaCertificates;
    }

    public List<byte[]> cdTrustKeys() {
        return cdTrustKeys;
    }

    private static List<byte[]> copy(List<byte[]> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<byte[]> output = new ArrayList<>(values.size());
        for (byte[] value : values) {
            if (value == null || value.length == 0) {
                continue;
            }
            output.add(value.clone());
        }
        return Collections.unmodifiableList(output);
    }
}
