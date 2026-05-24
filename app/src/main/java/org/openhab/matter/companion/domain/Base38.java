package org.openhab.matter.companion.domain;

import java.io.ByteArrayOutputStream;

public final class Base38 {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-.";

    private Base38() {
    }

    public static byte[] decode(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Base38 payload is required.");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int offset = 0;
        while (offset < text.length()) {
            int remaining = text.length() - offset;
            int groupSize = Math.min(5, remaining);
            if (groupSize == 1 || groupSize == 3) {
                throw new IllegalArgumentException("Invalid Base38 final group size.");
            }
            decodeGroup(text, offset, groupSize, output);
            offset += groupSize;
        }
        return output.toByteArray();
    }

    private static void decodeGroup(String text, int offset, int groupSize, ByteArrayOutputStream output) {
        long value = 0L;
        long multiplier = 1L;
        for (int index = 0; index < groupSize; index++) {
            char character = text.charAt(offset + index);
            int digit = ALPHABET.indexOf(character);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid Base38 character.");
            }
            value += digit * multiplier;
            multiplier *= ALPHABET.length();
        }

        int byteCount = bytesForGroup(groupSize);
        long maxValue = 1L << (byteCount * 8);
        if (value >= maxValue) {
            throw new IllegalArgumentException("Base38 group value exceeds byte capacity.");
        }
        for (int index = 0; index < byteCount; index++) {
            output.write((int) ((value >> (index * 8)) & 0xFF));
        }
    }

    private static int bytesForGroup(int groupSize) {
        if (groupSize == 5) {
            return 3;
        }
        if (groupSize == 4) {
            return 2;
        }
        if (groupSize == 2) {
            return 1;
        }
        throw new IllegalArgumentException("Invalid Base38 group size.");
    }
}
