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
                config.openHabBaseUrl());
    }

    public AppConfig fromStoredValues(String threadDataset, String openHabBaseUrl) {
        String safeThreadDataset = threadDataset == null ? "" : threadDataset;
        if (!safeThreadDataset.startsWith(SecretCodec.ENCRYPTED_PREFIX)) {
            return new AppConfig(safeThreadDataset, openHabBaseUrl);
        }

        try {
            return new AppConfig(secretCodec.decode(safeThreadDataset), openHabBaseUrl);
        } catch (GeneralSecurityException e) {
            return new AppConfig("", openHabBaseUrl);
        }
    }

    public static final class StoredConfig {
        private final String threadDataset;
        private final String openHabBaseUrl;

        StoredConfig(String threadDataset, String openHabBaseUrl) {
            this.threadDataset = threadDataset == null ? "" : threadDataset;
            this.openHabBaseUrl = openHabBaseUrl == null ? "" : openHabBaseUrl;
        }

        public String threadDataset() {
            return threadDataset;
        }

        public String openHabBaseUrl() {
            return openHabBaseUrl;
        }
    }
}
