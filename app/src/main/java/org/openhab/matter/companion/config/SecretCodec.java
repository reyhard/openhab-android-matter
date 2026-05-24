package org.openhab.matter.companion.config;

import java.security.GeneralSecurityException;

public interface SecretCodec {
    String ENCRYPTED_PREFIX = "enc:v1:";

    String encode(String plaintext) throws GeneralSecurityException;

    String decode(String encoded) throws GeneralSecurityException;
}
