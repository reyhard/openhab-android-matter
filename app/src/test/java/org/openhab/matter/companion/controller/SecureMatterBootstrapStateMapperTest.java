package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.config.SecretCodec;
import org.junit.Test;

import java.security.GeneralSecurityException;

import static org.junit.Assert.assertEquals;

public class SecureMatterBootstrapStateMapperTest {
    @Test
    public void encryptsOpaqueControllerStateButKeepsNodeIdReadable() throws Exception {
        SecureMatterBootstrapStateMapper mapper = new SecureMatterBootstrapStateMapper(new FixedSecretCodec());

        SecureMatterBootstrapStateMapper.StoredBootstrapState stored = mapper.toStoredValues(
                new MatterBootstrapState(1234L, "fabric-secret-material", false));

        assertEquals(1234L, stored.bootstrapNodeId());
        assertEquals("enc:v1:encoded(fabric-secret-material)", stored.controllerState());
    }

    @Test
    public void decodesEncryptedOpaqueControllerState() {
        SecureMatterBootstrapStateMapper mapper = new SecureMatterBootstrapStateMapper(new FixedSecretCodec());

        MatterBootstrapState state = mapper.fromStoredValues(1234L, "enc:v1:encoded(fabric-secret-material)");

        assertEquals(1234L, state.bootstrapNodeId());
        assertEquals("fabric-secret-material", state.controllerState());
        assertEquals(false, state.stateUnreadable());
    }

    @Test
    public void decodeFailureClearsStateAndMarksUnreadable() {
        SecureMatterBootstrapStateMapper mapper = new SecureMatterBootstrapStateMapper(new FixedSecretCodec());

        MatterBootstrapState state = mapper.fromStoredValues(1234L, "enc:v1:broken");

        assertEquals(-1L, state.bootstrapNodeId());
        assertEquals("", state.controllerState());
        assertEquals(true, state.stateUnreadable());
    }

    @Test
    public void emptyStoredControllerStateLoadsAsEmptyReadableState() {
        SecureMatterBootstrapStateMapper mapper = new SecureMatterBootstrapStateMapper(new FixedSecretCodec());

        MatterBootstrapState state = mapper.fromStoredValues(-1L, "");

        assertEquals(-1L, state.bootstrapNodeId());
        assertEquals("", state.controllerState());
        assertEquals(false, state.stateUnreadable());
    }

    @Test
    public void emptyStoredControllerStatePreservesReadableNodeId() {
        SecureMatterBootstrapStateMapper mapper = new SecureMatterBootstrapStateMapper(new FixedSecretCodec());

        MatterBootstrapState state = mapper.fromStoredValues(1234L, "");

        assertEquals(1234L, state.bootstrapNodeId());
        assertEquals("", state.controllerState());
        assertEquals(false, state.stateUnreadable());
    }

    private static final class FixedSecretCodec implements SecretCodec {
        @Override
        public String encode(String plaintext) {
            if (plaintext == null || plaintext.isEmpty()) {
                return "";
            }
            return "enc:v1:encoded(" + plaintext + ")";
        }

        @Override
        public String decode(String encoded) throws GeneralSecurityException {
            if (encoded == null || encoded.isEmpty()) {
                return "";
            }
            if (!encoded.startsWith("enc:v1:encoded(") || !encoded.endsWith(")")) {
                throw new GeneralSecurityException("broken");
            }
            return encoded.substring("enc:v1:encoded(".length(), encoded.length() - 1);
        }
    }
}
