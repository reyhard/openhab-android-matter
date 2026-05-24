package org.openhab.matter.companion.config;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public final class AndroidKeystoreSecretCodec implements SecretCodec {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "openhab_matter_thread_dataset";

    @Override
    public String encode(String plaintext) throws GeneralSecurityException {
        return delegate().encode(plaintext);
    }

    @Override
    public String decode(String encoded) throws GeneralSecurityException {
        return delegate().decode(encoded);
    }

    private AesGcmSecretCodec delegate() throws GeneralSecurityException {
        return new AesGcmSecretCodec(loadOrCreateKey());
    }

    private SecretKey loadOrCreateKey() throws GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        try {
            keyStore.load(null);
            SecretKey existingKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            if (existingKey != null) {
                return existingKey;
            }

            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build());
            return keyGenerator.generateKey();
        } catch (GeneralSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralSecurityException("Unable to load Android Keystore key", e);
        }
    }
}
