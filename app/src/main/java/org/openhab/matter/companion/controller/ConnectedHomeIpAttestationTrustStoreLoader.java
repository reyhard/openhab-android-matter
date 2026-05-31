package org.openhab.matter.companion.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConnectedHomeIpAttestationTrustStoreLoader {
    public interface AssetLister {
        String[] list(String path) throws IOException;
    }

    public interface AssetOpener {
        InputStream open(String path) throws IOException;
    }

    private final AssetLister listAssets;
    private final AssetOpener openAsset;

    public ConnectedHomeIpAttestationTrustStoreLoader(AssetLister listAssets, AssetOpener openAsset) {
        if (listAssets == null) {
            throw new IllegalArgumentException("listAssets is required");
        }
        if (openAsset == null) {
            throw new IllegalArgumentException("openAsset is required");
        }
        this.listAssets = listAssets;
        this.openAsset = openAsset;
    }

    public ConnectedHomeIpAttestationTrustStore load(String paaPath, String cdPath) {
        return new ConnectedHomeIpAttestationTrustStore(
                readCertificates(paaPath),
                readCertificates(cdPath));
    }

    private List<byte[]> readCertificates(String rootPath) {
        List<byte[]> certificates = new ArrayList<>();
        Set<String> certificateKeys = new HashSet<>();
        if (rootPath == null || rootPath.isEmpty()) {
            return certificates;
        }
        try {
            String[] names = listAssets.list(rootPath);
            if (names == null) {
                return certificates;
            }
            for (String name : names) {
                if (name == null || name.isEmpty()) {
                    continue;
                }
                String fullPath = rootPath + "/" + name;
                try (InputStream input = openAsset.open(fullPath)) {
                    byte[] raw = readFully(input);
                    byte[] parsed = parseCertificate(raw);
                    String certificateKey = Base64.getEncoder().encodeToString(parsed);
                    if (parsed.length > 0 && certificateKeys.add(certificateKey)) {
                        certificates.add(parsed);
                    }
                }
            }
        } catch (IOException exception) {
            ConnectedHomeIpDiagnostics.emit("Attestation trust-store asset path unavailable: " + rootPath);
        }
        return certificates;
    }

    private static byte[] parseCertificate(byte[] value) {
        String text = new String(value, StandardCharsets.US_ASCII);
        if (text.contains("-----BEGIN CERTIFICATE-----")) {
            String stripped = text
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "");
            return Base64.getDecoder().decode(stripped);
        }
        return value;
    }

    private static byte[] readFully(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        while (true) {
            int read = input.read(buffer);
            if (read < 0) {
                return out.toByteArray();
            }
            out.write(buffer, 0, read);
        }
    }
}
