package org.openhab.matter.companion.config;

import org.junit.Test;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AesGcmSecretCodecTest {
    @Test
    public void encodedValueDoesNotContainPlaintextAndCanBeDecoded() throws Exception {
        SecretKey key = new SecretKeySpec(new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16
        }, "AES");
        SecretCodec codec = new AesGcmSecretCodec(key, new SecureRandom(new byte[] {9, 8, 7, 6}));

        String encoded = codec.encode("hex:00112233445566778899aabbccddeeff");

        assertTrue(encoded.startsWith("enc:v1:"));
        assertFalse(encoded.contains("00112233445566778899"));
        assertEquals("hex:00112233445566778899aabbccddeeff", codec.decode(encoded));
    }

    @Test(expected = GeneralSecurityException.class)
    public void tamperedValueCannotBeDecoded() throws Exception {
        SecretKey key = new SecretKeySpec(new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16
        }, "AES");
        SecretCodec codec = new AesGcmSecretCodec(key, new SecureRandom(new byte[] {1, 2, 3, 4}));

        String encoded = codec.encode("hex:00112233445566778899aabbccddeeff");
        codec.decode(encoded.substring(0, encoded.length() - 2) + "AA");
    }
}
