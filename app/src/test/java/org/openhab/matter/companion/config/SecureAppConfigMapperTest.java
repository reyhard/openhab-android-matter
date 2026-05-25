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
                new AppConfig("hex:001122", "http://openhab.local:8080", "http://otbr.local"));

        assertEquals("enc:v1:encoded(hex:001122)", stored.threadDataset());
        assertEquals("http://openhab.local:8080", stored.openHabBaseUrl());
        assertEquals("http://otbr.local", stored.otbrBaseUrl());
    }

    @Test
    public void decodesStoredEncryptedThreadDatasetAndOtbrUrl() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues("enc:v1:encoded(hex:001122)",
                "http://openhab.local:8080",
                "http://otbr.local");

        assertEquals("hex:001122", config.threadDataset());
        assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
        assertEquals("http://otbr.local", config.otbrBaseUrl());
    }

    @Test
    public void preservesAttestationBypassWhenDecodingStoredConfig() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(
                "enc:v1:encoded(hex:001122)",
                "http://openhab.local:8080",
                "http://otbr.local",
                true);

        assertEquals(true, config.attestationBypassEnabled());
    }

    @Test
    public void legacyMapperCallsDefaultAttestationBypassToFalse() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(
                "enc:v1:encoded(hex:001122)",
                "http://openhab.local:8080",
                "http://otbr.local");

        assertEquals(false, config.attestationBypassEnabled());
    }

    @Test
    public void readsLegacyPlaintextThreadDatasetForMigration() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues("hex:legacy", "http://openhab.local:8080", "http://otbr.local");

        assertEquals("hex:legacy", config.threadDataset());
        assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
        assertEquals("http://otbr.local", config.otbrBaseUrl());
    }

    @Test
    public void preservesAttestationBypassForLegacyPlaintextThreadDataset() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues("hex:legacy", "http://openhab.local:8080", "http://otbr.local",
                true);

        assertEquals(true, config.attestationBypassEnabled());
    }

    @Test
    public void preservesAttestationBypassForEmptyThreadDataset() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues("", "http://openhab.local:8080", "http://otbr.local", true);

        assertEquals(true, config.attestationBypassEnabled());
    }

    @Test
    public void decodeFailureClearsThreadDatasetAndKeepsOpenHabUrl() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(SecretCodec.ENCRYPTED_PREFIX + "broken",
                "http://openhab.local:8080",
                "http://otbr.local");

        assertEquals("", config.threadDataset());
        assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
        assertEquals("http://otbr.local", config.otbrBaseUrl());
        assertEquals(true, config.threadDatasetUnreadable());
    }

    @Test
    public void preservesAttestationBypassWhenDecodeFails() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(
                SecretCodec.ENCRYPTED_PREFIX + "broken",
                "http://openhab.local:8080",
                "http://otbr.local",
                true);

        assertEquals(true, config.attestationBypassEnabled());
    }

    @Test
    public void detectsLegacyPlaintextThreadDataset() {
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(new FixedSecretCodec());

        assertEquals(true, mapper.isLegacyPlaintextThreadDataset("hex:legacy"));
        assertEquals(false, mapper.isLegacyPlaintextThreadDataset(""));
        assertEquals(false, mapper.isLegacyPlaintextThreadDataset("enc:v1:encoded(hex:001122)"));
    }

    private static final class FixedSecretCodec implements SecretCodec {
        @Override
        public String encode(String plaintext) {
            return "enc:v1:encoded(" + plaintext + ")";
        }

        @Override
        public String decode(String encoded) throws GeneralSecurityException {
            if (!encoded.startsWith("enc:v1:encoded(") || !encoded.endsWith(")")) {
                throw new GeneralSecurityException("broken");
            }
            return encoded.substring("enc:v1:encoded(".length(), encoded.length() - 1);
        }
    }
}
