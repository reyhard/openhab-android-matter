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
                secretCodec.encode(config.setupPayload()),
                config.openHabBaseUrl(),
                secretCodec.encode(config.openHabApiToken()),
                config.otbrBaseUrl());
    }

    public AppConfig fromStoredValues(String threadDataset, String openHabBaseUrl) {
        return fromStoredValues(threadDataset, openHabBaseUrl, "");
    }

    public AppConfig fromStoredValues(String threadDataset, String openHabBaseUrl, String otbrBaseUrl) {
        return fromStoredValues(threadDataset, openHabBaseUrl, "", otbrBaseUrl, false);
    }

    public AppConfig fromStoredValues(
            String threadDataset,
            String setupPayload,
            String openHabBaseUrl,
            String openHabApiToken,
            String otbrBaseUrl) {
        return fromStoredValues(threadDataset, setupPayload, openHabBaseUrl, openHabApiToken, otbrBaseUrl, false);
    }

    public AppConfig fromStoredValues(
            String threadDataset,
            String openHabBaseUrl,
            String openHabApiToken,
            String otbrBaseUrl) {
        return fromStoredValues(threadDataset, openHabBaseUrl, openHabApiToken, otbrBaseUrl, false);
    }

    public AppConfig fromStoredValues(
            String threadDataset,
            String openHabBaseUrl,
            String otbrBaseUrl,
            boolean attestationBypassEnabled) {
        return fromStoredValues(threadDataset, openHabBaseUrl, "", otbrBaseUrl, attestationBypassEnabled);
    }

    public AppConfig fromStoredValues(
            String threadDataset,
            String openHabBaseUrl,
            String openHabApiToken,
            String otbrBaseUrl,
            boolean attestationBypassEnabled) {
        return fromStoredValues(threadDataset, "", openHabBaseUrl, openHabApiToken, otbrBaseUrl,
                attestationBypassEnabled);
    }

    public AppConfig fromStoredValues(
            String threadDataset,
            String setupPayload,
            String openHabBaseUrl,
            String openHabApiToken,
            String otbrBaseUrl,
            boolean attestationBypassEnabled) {
        String safeThreadDataset = threadDataset == null ? "" : threadDataset;
        DecodedSecret decodedThreadDataset = decodeThreadDataset(safeThreadDataset);
        DecodedSecret decodedSetupPayload = decodeStoredSecret(setupPayload);
        DecodedSecret decodedToken = decodeStoredSecret(openHabApiToken);
        return new AppConfig(
                decodedThreadDataset.value,
                decodedSetupPayload.value,
                openHabBaseUrl,
                decodedToken.value,
                otbrBaseUrl,
                decodedThreadDataset.unreadable,
                decodedSetupPayload.unreadable,
                decodedToken.unreadable,
                attestationBypassEnabled);
    }

    public boolean isLegacyPlaintextThreadDataset(String threadDataset) {
        String safeThreadDataset = threadDataset == null ? "" : threadDataset;
        return !safeThreadDataset.isEmpty() && !safeThreadDataset.startsWith(SecretCodec.ENCRYPTED_PREFIX);
    }

    public boolean isLegacyPlaintextOpenHabApiToken(String openHabApiToken) {
        String safeOpenHabApiToken = openHabApiToken == null ? "" : openHabApiToken;
        return !safeOpenHabApiToken.isEmpty() && !safeOpenHabApiToken.startsWith(SecretCodec.ENCRYPTED_PREFIX);
    }

    private DecodedSecret decodeThreadDataset(String threadDataset) {
        if (isLegacyPlaintextThreadDataset(threadDataset)) {
            return new DecodedSecret(threadDataset, false);
        }
        return decodeStoredSecret(threadDataset);
    }

    private DecodedSecret decodeStoredSecret(String encoded) {
        String safeEncoded = encoded == null ? "" : encoded;
        if (safeEncoded.isEmpty()) {
            return new DecodedSecret("", false);
        }
        if (!safeEncoded.startsWith(SecretCodec.ENCRYPTED_PREFIX)) {
            return new DecodedSecret(safeEncoded, false);
        }
        try {
            return new DecodedSecret(secretCodec.decode(safeEncoded), false);
        } catch (GeneralSecurityException e) {
            return new DecodedSecret("", true);
        }
    }

    private static final class DecodedSecret {
        private final String value;
        private final boolean unreadable;

        DecodedSecret(String value, boolean unreadable) {
            this.value = value == null ? "" : value;
            this.unreadable = unreadable;
        }
    }

    public static final class StoredConfig {
        private final String threadDataset;
        private final String setupPayload;
        private final String openHabBaseUrl;
        private final String openHabApiToken;
        private final String otbrBaseUrl;

        StoredConfig(
                String threadDataset,
                String setupPayload,
                String openHabBaseUrl,
                String openHabApiToken,
                String otbrBaseUrl) {
            this.threadDataset = threadDataset == null ? "" : threadDataset;
            this.setupPayload = setupPayload == null ? "" : setupPayload;
            this.openHabBaseUrl = openHabBaseUrl == null ? "" : openHabBaseUrl;
            this.openHabApiToken = openHabApiToken == null ? "" : openHabApiToken;
            this.otbrBaseUrl = otbrBaseUrl == null ? "" : otbrBaseUrl;
        }

        public String threadDataset() {
            return threadDataset;
        }

        public String setupPayload() {
            return setupPayload;
        }

        public String openHabBaseUrl() {
            return openHabBaseUrl;
        }

        public String openHabApiToken() {
            return openHabApiToken;
        }

        public String otbrBaseUrl() {
            return otbrBaseUrl;
        }
    }
}
