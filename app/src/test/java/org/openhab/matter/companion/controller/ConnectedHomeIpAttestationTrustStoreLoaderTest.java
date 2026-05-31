package org.openhab.matter.companion.controller;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ConnectedHomeIpAttestationTrustStoreLoaderTest {
    @Test
    public void loadsPemAndDerCertificatesFromAssets() throws Exception {
        FakeAssets assets = new FakeAssets();
        assets.add("matter/truststore/paa", "vendorA.pem", pem("AQID"));
        assets.add("matter/truststore/paa", "vendorB.der", new byte[] {4, 5, 6});

        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStoreLoader(assets::list, assets::open)
                        .load("matter/truststore/paa", "matter/truststore/cd");

        assertEquals(2, store.paaCertificates().size());
        assertEquals(0, store.cdTrustKeys().size());
    }

    @Test
    public void loadsCdTrustKeysWhenPresent() throws Exception {
        FakeAssets assets = new FakeAssets();
        assets.add("matter/truststore/cd", "cd1.der", new byte[] {9, 8, 7});

        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStoreLoader(assets::list, assets::open)
                        .load("matter/truststore/paa", "matter/truststore/cd");

        assertEquals(0, store.paaCertificates().size());
        assertEquals(1, store.cdTrustKeys().size());
    }

    @Test
    public void deduplicatesCertificatesAfterPemDecoding() throws Exception {
        FakeAssets assets = new FakeAssets();
        assets.add("matter/truststore/cd", "cd1.der", new byte[] {9, 8, 7});
        assets.add("matter/truststore/cd", "cd1.pem", pem("CQgH"));

        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStoreLoader(assets::list, assets::open)
                        .load("matter/truststore/paa", "matter/truststore/cd");

        assertEquals(1, store.cdTrustKeys().size());
    }

    @Test
    public void missingAssetDirectoriesReturnEmptyStore() throws Exception {
        FakeAssets assets = new FakeAssets();

        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStoreLoader(assets::list, assets::open)
                        .load("matter/truststore/paa", "matter/truststore/cd");

        assertTrue(store.paaCertificates().isEmpty());
        assertTrue(store.cdTrustKeys().isEmpty());
    }

    private static byte[] pem(String base64Body) {
        String text = "-----BEGIN CERTIFICATE-----\n"
                + base64Body
                + "\n-----END CERTIFICATE-----\n";
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    private static final class FakeAssets {
        private final Map<String, Map<String, byte[]>> entries = new HashMap<>();

        void add(String dir, String name, byte[] value) {
            entries.computeIfAbsent(dir, ignored -> new HashMap<>()).put(name, value);
        }

        String[] list(String dir) {
            Map<String, byte[]> files = entries.get(dir);
            return files == null ? new String[0] : files.keySet().toArray(new String[0]);
        }

        InputStream open(String path) throws IOException {
            int slash = path.lastIndexOf('/');
            String dir = slash < 0 ? "" : path.substring(0, slash);
            String name = slash < 0 ? path : path.substring(slash + 1);
            Map<String, byte[]> files = entries.get(dir);
            if (files == null || !files.containsKey(name)) {
                throw new IOException("missing " + path);
            }
            return new ByteArrayInputStream(files.get(name));
        }
    }
}
