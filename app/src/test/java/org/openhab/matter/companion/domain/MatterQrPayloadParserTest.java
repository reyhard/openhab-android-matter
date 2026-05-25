package org.openhab.matter.companion.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class MatterQrPayloadParserTest {
    private static final String BASE38_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-.";

    @Test
    public void parsesKnownProjectChipQrPayload() {
        MatterSetupPayload payload = MatterSetupPayloadParser.parse("MT:Y.K9042C00KA0648G00");

        assertEquals("MT:Y.K9042C00KA0648G00", payload.rawPayload());
        assertEquals(20202021L, payload.pin());
        assertEquals(3840, payload.discriminator());
        assertEquals(65521, payload.vendorId());
        assertEquals(32768, payload.productId());
        assertEquals(0, payload.commissioningFlow());
        assertEquals(2, payload.discoveryCapabilities());
        assertFalse(payload.requiresChipParser());
    }

    @Test
    public void rejectsQrPayloadWithWrongPrefix() {
        assertThrows(IllegalArgumentException.class, () -> MatterSetupPayloadParser.parse("XX:Y.K9042C00KA0648G00"));
    }

    @Test
    public void rejectsQrPayloadWithInvalidBase38Character() {
        assertThrows(IllegalArgumentException.class, () -> MatterSetupPayloadParser.parse("MT:Y.K9042C00KA0648G0_"));
    }

    @Test
    public void rejectsQrPayloadWithTrailingBytes() {
        assertThrows(IllegalArgumentException.class, () -> MatterSetupPayloadParser.parse("MT:Y.K9042C00KA0648G000"));
    }

    @Test
    public void rejectsQrPayloadWithInvalidSetupPin() {
        assertThrows(IllegalArgumentException.class, () -> MatterSetupPayloadParser.parse(qrPayloadWithPin(0)));
        assertThrows(IllegalArgumentException.class, () -> MatterSetupPayloadParser.parse(qrPayloadWithPin(100000000)));
    }

    static String qrPayloadWithPin(long pin) {
        byte[] bytes = new byte[11];
        writeBits(bytes, 0, 3, 0);
        writeBits(bytes, 3, 16, 65521);
        writeBits(bytes, 19, 16, 32768);
        writeBits(bytes, 35, 2, 0);
        writeBits(bytes, 37, 8, 2);
        writeBits(bytes, 45, 12, 3840);
        writeBits(bytes, 57, 27, pin);
        writeBits(bytes, 84, 4, 0);
        return "MT:" + encodeBase38(bytes);
    }

    private static void writeBits(byte[] bytes, int bitOffset, int bitCount, long value) {
        for (int index = 0; index < bitCount; index++) {
            if (((value >> index) & 1L) != 0L) {
                int absoluteBit = bitOffset + index;
                bytes[absoluteBit / 8] = (byte) (bytes[absoluteBit / 8] | (1 << (absoluteBit % 8)));
            }
        }
    }

    private static String encodeBase38(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        int offset = 0;
        while (offset < bytes.length) {
            int remaining = bytes.length - offset;
            int byteCount = Math.min(3, remaining);
            long value = 0L;
            for (int index = 0; index < byteCount; index++) {
                value |= (long) (bytes[offset + index] & 0xFF) << (8 * index);
            }
            int characterCount = byteCount == 3 ? 5 : byteCount == 2 ? 4 : 2;
            for (int index = 0; index < characterCount; index++) {
                builder.append(BASE38_ALPHABET.charAt((int) (value % BASE38_ALPHABET.length())));
                value /= BASE38_ALPHABET.length();
            }
            offset += byteCount;
        }
        return builder.toString();
    }
}
