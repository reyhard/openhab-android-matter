package org.openhab.matter.companion.config;

import org.junit.Test;

import java.security.GeneralSecurityException;

import static org.junit.Assert.assertEquals;

public class SecureAppConfigMapperTest {
    @Test
    public void encodesThreadDatasetButLeavesOpenHabUrlPlaintext() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        SecureAppConfigMapper.StoredConfig stored = mapper.toStoredValues(
                new AppConfig("hex:001122", "http://openhab.local:8080"));

        assertEquals("encoded(hex:001122)", stored.threadDataset());
        assertEquals("http://openhab.local:8080", stored.openHabBaseUrl());
    }

    @Test
    public void readsLegacyPlaintextThreadDatasetForMigration() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues("hex:legacy", "http://openhab.local:8080");

        assertEquals("hex:legacy", config.threadDataset());
        assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
    }

    @Test
    public void decodeFailureClearsThreadDatasetAndKeepsOpenHabUrl() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(SecretCodec.ENCRYPTED_PREFIX + "broken",
                "http://openhab.local:8080");

        assertEquals("", config.threadDataset());
        assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
    }

    private static final class FixedSecretCodec implements SecretCodec {
        @Override
        public String encode(String plaintext) {
            return "encoded(" + plaintext + ")";
        }

        @Override
        public String decode(String encoded) throws GeneralSecurityException {
            if (!encoded.startsWith("encoded(") || !encoded.endsWith(")")) {
                throw new GeneralSecurityException("broken");
            }
            return encoded.substring("encoded(".length(), encoded.length() - 1);
        }
    }
}
