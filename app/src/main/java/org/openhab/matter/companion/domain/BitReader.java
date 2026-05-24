package org.openhab.matter.companion.domain;

public final class BitReader {
    private final byte[] bytes;
    private int bitOffset;

    public BitReader(byte[] bytes) {
        this.bytes = bytes.clone();
    }

    public long readUnsigned(int bitCount) {
        if (bitCount < 0 || bitCount > 63) {
            throw new IllegalArgumentException("Bit count must be between 0 and 63.");
        }
        if (bitOffset + bitCount > bytes.length * 8) {
            throw new IllegalArgumentException("Not enough bits in payload.");
        }

        long value = 0L;
        for (int index = 0; index < bitCount; index++) {
            int absoluteBit = bitOffset + index;
            int currentByte = bytes[absoluteBit / 8] & 0xFF;
            int bit = (currentByte >> (absoluteBit % 8)) & 1;
            value |= (long) bit << index;
        }
        bitOffset += bitCount;
        return value;
    }
}
