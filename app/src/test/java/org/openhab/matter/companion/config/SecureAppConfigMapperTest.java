package org.openhab.matter.companion.config;

import org.junit.Test;

import java.security.GeneralSecurityException;

import static org.junit.Assert.assertEquals;

public class SecureAppConfigMapperTest {
    @Test
    public void encodesThreadDatasetAndOpenHabTokenButLeavesUrlsPlaintext() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        SecureAppConfigMapper.StoredConfig stored = mapper.toStoredValues(
                new AppConfig("hex:001122", "MT:ABC", "http://openhab.local:8080", "oh.test.token", "fd00::1"));

        assertEquals("enc:v1:encoded(hex:001122)", stored.threadDataset());
        assertEquals("enc:v1:encoded(MT:ABC)", stored.setupPayload());
        assertEquals("http://openhab.local:8080", stored.openHabBaseUrl());
        assertEquals("enc:v1:encoded(oh.test.token)", stored.openHabApiToken());
        assertEquals("fd00::1", stored.otbrBaseUrl());
    }

    @Test
    public void decodesStoredEncryptedThreadDatasetAndOtbrUrl() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues("enc:v1:encoded(hex:001122)",
                "enc:v1:encoded(MT:ABC)",
                "http://openhab.local:8080",
                "enc:v1:encoded(oh.test.token)",
                "http://otbr.local");

        assertEquals("hex:001122", config.threadDataset());
        assertEquals("MT:ABC", config.setupPayload());
        assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
        assertEquals("oh.test.token", config.openHabApiToken());
        assertEquals("http://otbr.local", config.otbrBaseUrl());
    }

    @Test
    public void setupPayloadDecodeFailureClearsPayloadAndMarksItUnreadable() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(
                "enc:v1:encoded(hex:001122)",
                SecretCodec.ENCRYPTED_PREFIX + "broken",
                "http://openhab.local:8080",
                "",
                "http://otbr.local");

        assertEquals("", config.setupPayload());
        assertEquals(true, config.setupPayloadUnreadable());
    }

    @Test
    public void preservesAttestationBypassWhenDecodingStoredConfig() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(
                "enc:v1:encoded(hex:001122)",
                "http://openhab.local:8080",
                "enc:v1:encoded(oh.test.token)",
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
                "",
                "http://otbr.local");

        assertEquals(false, config.attestationBypassEnabled());
    }

    @Test
    public void readsLegacyPlaintextThreadDatasetForMigration() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues("hex:legacy", "http://openhab.local:8080", "", "http://otbr.local");

        assertEquals("hex:legacy", config.threadDataset());
        assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
        assertEquals("http://otbr.local", config.otbrBaseUrl());
    }

    @Test
    public void preservesAttestationBypassForLegacyPlaintextThreadDataset() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues("hex:legacy", "http://openhab.local:8080", "", "http://otbr.local",
                true);

        assertEquals(true, config.attestationBypassEnabled());
    }

    @Test
    public void preservesAttestationBypassForEmptyThreadDataset() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues("", "http://openhab.local:8080", "", "http://otbr.local", true);

        assertEquals(true, config.attestationBypassEnabled());
    }

    @Test
    public void decodeFailureClearsThreadDatasetAndKeepsOpenHabUrl() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(SecretCodec.ENCRYPTED_PREFIX + "broken",
                "http://openhab.local:8080",
                "enc:v1:encoded(oh.test.token)",
                "http://otbr.local");

        assertEquals("", config.threadDataset());
        assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
        assertEquals("oh.test.token", config.openHabApiToken());
        assertEquals("http://otbr.local", config.otbrBaseUrl());
        assertEquals(true, config.threadDatasetUnreadable());
    }

    @Test
    public void tokenDecodeFailureClearsTokenAndMarksItUnreadable() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(
                "enc:v1:encoded(hex:001122)",
                "http://openhab.local:8080",
                SecretCodec.ENCRYPTED_PREFIX + "broken",
                "http://otbr.local");

        assertEquals("hex:001122", config.threadDataset());
        assertEquals("", config.openHabApiToken());
        assertEquals(true, config.openHabApiTokenUnreadable());
    }

    @Test
    public void preservesAttestationBypassWhenDecodeFails() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(
                SecretCodec.ENCRYPTED_PREFIX + "broken",
                "http://openhab.local:8080",
                "",
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

    @Test
    public void detectsLegacyPlaintextOpenHabApiToken() {
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(new FixedSecretCodec());

        assertEquals(true, mapper.isLegacyPlaintextOpenHabApiToken("oh.legacy.token"));
        assertEquals(false, mapper.isLegacyPlaintextOpenHabApiToken(""));
        assertEquals(false, mapper.isLegacyPlaintextOpenHabApiToken("enc:v1:encoded(oh.test.token)"));
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
