package org.openhab.matter.companion.config;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class AesGcmSecretCodec implements SecretCodec {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;

    public AesGcmSecretCodec(SecretKey key) {
        this.key = key;
    }

    public AesGcmSecretCodec(SecretKey key, @SuppressWarnings("unused") java.security.SecureRandom secureRandom) {
        this(key);
    }

    @Override
    public String encode(String plaintext) throws GeneralSecurityException {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        if (iv == null || iv.length != IV_BYTES) {
            throw new GeneralSecurityException("Unexpected GCM IV length");
        }

        Base64.Encoder encoder = Base64.getEncoder();
        return ENCRYPTED_PREFIX + encoder.encodeToString(iv) + ":" + encoder.encodeToString(ciphertext);
    }

    @Override
    public String decode(String encoded) throws GeneralSecurityException {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        if (!encoded.startsWith(ENCRYPTED_PREFIX)) {
            throw new GeneralSecurityException("Missing encrypted value prefix");
        }

        String payload = encoded.substring(ENCRYPTED_PREFIX.length());
        String[] parts = payload.split(":", -1);
        if (parts.length != 2) {
            throw new GeneralSecurityException("Malformed encrypted value");
        }

        try {
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[1]);
            if (iv.length != IV_BYTES || ciphertext.length == 0) {
                throw new GeneralSecurityException("Malformed encrypted value");
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Malformed encrypted value", e);
        }
    }
}
