package org.openhab.matter.companion.config;

import java.security.GeneralSecurityException;

public final class SecureAppConfigMapper {
    private final SecretCodec secretCodec;

    public SecureAppConfigMapper(SecretCodec secretCodec) {
        this.secretCodec = secretCodec;
    }

    public StoredConfig toStoredValues(AppConfig config) throws GeneralSecurityException {
        return new StoredConfig(
                secretCodec.encode(config.threadDataset()),
                config.openHabBaseUrl(),
                config.otbrBaseUrl());
    }

    public AppConfig fromStoredValues(String threadDataset, String openHabBaseUrl) {
        return fromStoredValues(threadDataset, openHabBaseUrl, "");
    }

    public AppConfig fromStoredValues(String threadDataset, String openHabBaseUrl, String otbrBaseUrl) {
        String safeThreadDataset = threadDataset == null ? "" : threadDataset;
        if (isLegacyPlaintextThreadDataset(safeThreadDataset)) {
            return new AppConfig(safeThreadDataset, openHabBaseUrl, otbrBaseUrl);
        }
        if (safeThreadDataset.isEmpty()) {
            return new AppConfig("", openHabBaseUrl, otbrBaseUrl);
        }

        try {
            return new AppConfig(secretCodec.decode(safeThreadDataset), openHabBaseUrl, otbrBaseUrl);
        } catch (GeneralSecurityException e) {
            return new AppConfig("", openHabBaseUrl, otbrBaseUrl, true);
        }
    }

    public boolean isLegacyPlaintextThreadDataset(String threadDataset) {
        String safeThreadDataset = threadDataset == null ? "" : threadDataset;
        return !safeThreadDataset.isEmpty() && !safeThreadDataset.startsWith(SecretCodec.ENCRYPTED_PREFIX);
    }

    public static final class StoredConfig {
        private final String threadDataset;
        private final String openHabBaseUrl;
        private final String otbrBaseUrl;

        StoredConfig(String threadDataset, String openHabBaseUrl, String otbrBaseUrl) {
            this.threadDataset = threadDataset == null ? "" : threadDataset;
            this.openHabBaseUrl = openHabBaseUrl == null ? "" : openHabBaseUrl;
            this.otbrBaseUrl = otbrBaseUrl == null ? "" : otbrBaseUrl;
        }

        public String threadDataset() {
            return threadDataset;
        }

        public String openHabBaseUrl() {
            return openHabBaseUrl;
        }

        public String otbrBaseUrl() {
            return otbrBaseUrl;
        }
    }
}
