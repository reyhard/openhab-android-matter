package org.openhab.matter.companion.qr;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class QrCodeMatrix {
    private static final int[] DATA_CODEWORDS = {19, 34, 55, 80};
    private static final int[] ECC_CODEWORDS = {7, 10, 15, 20};
    private static final int[][] ALIGNMENT_POSITIONS = {
            {},
            {6, 18},
            {6, 22},
            {6, 26}
    };

    private final boolean[][] modules;

    private QrCodeMatrix(boolean[][] modules) {
        this.modules = copy(modules);
    }

    public static QrCodeMatrix encodeText(String text) {
        byte[] bytes = nullToEmpty(text).getBytes(StandardCharsets.UTF_8);
        int version = selectVersion(bytes.length);
        int dataCodewords = DATA_CODEWORDS[version - 1];
        int eccCodewords = ECC_CODEWORDS[version - 1];
        byte[] data = encodeData(bytes, dataCodewords);
        byte[] codewords = appendEcc(data, eccCodewords);
        return new QrCodeMatrix(buildMatrix(version, codewords));
    }

    public int size() {
        return modules.length;
    }

    public boolean[][] modules() {
        return copy(modules);
    }

    private static int selectVersion(int byteLength) {
        int requiredBits = 4 + 8 + byteLength * 8;
        for (int index = 0; index < DATA_CODEWORDS.length; index++) {
            if (requiredBits <= DATA_CODEWORDS[index] * 8) {
                return index + 1;
            }
        }
        throw new IllegalArgumentException("QR payload is too long for the built-in encoder");
    }

    private static byte[] encodeData(byte[] bytes, int dataCodewords) {
        BitBuffer bits = new BitBuffer();
        bits.append(0b0100, 4);
        bits.append(bytes.length, 8);
        for (byte value : bytes) {
            bits.append(value & 0xFF, 8);
        }
        int capacityBits = dataCodewords * 8;
        bits.append(0, Math.min(4, capacityBits - bits.size()));
        while (bits.size() % 8 != 0) {
            bits.append(0, 1);
        }

        byte[] data = new byte[dataCodewords];
        int written = bits.toBytes(data);
        int pad = 0xEC;
        while (written < data.length) {
            data[written++] = (byte) pad;
            pad = pad == 0xEC ? 0x11 : 0xEC;
        }
        return data;
    }

    private static byte[] appendEcc(byte[] data, int eccCodewords) {
        byte[] ecc = reedSolomonRemainder(data, eccCodewords);
        byte[] result = Arrays.copyOf(data, data.length + ecc.length);
        System.arraycopy(ecc, 0, result, data.length, ecc.length);
        return result;
    }

    private static byte[] reedSolomonRemainder(byte[] data, int degree) {
        int[] generator = reedSolomonGenerator(degree);
        int[] remainder = new int[degree];
        for (byte value : data) {
            int factor = (value & 0xFF) ^ remainder[0];
            System.arraycopy(remainder, 1, remainder, 0, degree - 1);
            remainder[degree - 1] = 0;
            for (int index = 0; index < degree; index++) {
                remainder[index] ^= gfMultiply(generator[index + 1], factor);
            }
        }
        byte[] result = new byte[degree];
        for (int index = 0; index < degree; index++) {
            result[index] = (byte) remainder[index];
        }
        return result;
    }

    private static int[] reedSolomonGenerator(int degree) {
        int[] coefficients = {1};
        for (int index = 0; index < degree; index++) {
            int[] next = new int[coefficients.length + 1];
            for (int term = 0; term < coefficients.length; term++) {
                next[term] ^= coefficients[term];
                next[term + 1] ^= gfMultiply(coefficients[term], gfPow(index));
            }
            coefficients = next;
        }
        return coefficients;
    }

    private static boolean[][] buildMatrix(int version, byte[] codewords) {
        int size = 21 + (version - 1) * 4;
        boolean[][] modules = new boolean[size][size];
        boolean[][] reserved = new boolean[size][size];
        drawFunctionPatterns(version, modules, reserved);
        placeData(codewords, modules, reserved);
        applyMask(modules, reserved);
        drawFormatBits(modules, reserved);
        return modules;
    }

    private static void drawFunctionPatterns(int version, boolean[][] modules, boolean[][] reserved) {
        int size = modules.length;
        drawFinder(modules, reserved, 0, 0);
        drawFinder(modules, reserved, 0, size - 7);
        drawFinder(modules, reserved, size - 7, 0);
        for (int index = 8; index < size - 8; index++) {
            setFunction(modules, reserved, 6, index, index % 2 == 0);
            setFunction(modules, reserved, index, 6, index % 2 == 0);
        }
        if (version > 1) {
            int[] positions = ALIGNMENT_POSITIONS[version - 1];
            for (int row : positions) {
                for (int column : positions) {
                    if (!isFinderOverlap(size, row, column)) {
                        drawAlignment(modules, reserved, row, column);
                    }
                }
            }
        }
        setFunction(modules, reserved, 4 * version + 9, 8, true);
        reserveFormatAreas(reserved);
    }

    private static void drawFinder(boolean[][] modules, boolean[][] reserved, int row, int column) {
        for (int y = -1; y <= 7; y++) {
            for (int x = -1; x <= 7; x++) {
                int r = row + y;
                int c = column + x;
                if (r < 0 || c < 0 || r >= modules.length || c >= modules.length) {
                    continue;
                }
                boolean dark = y >= 0 && y <= 6 && x >= 0 && x <= 6
                        && (y == 0 || y == 6 || x == 0 || x == 6 || (y >= 2 && y <= 4 && x >= 2 && x <= 4));
                setFunction(modules, reserved, r, c, dark);
            }
        }
    }

    private static void drawAlignment(boolean[][] modules, boolean[][] reserved, int row, int column) {
        for (int y = -2; y <= 2; y++) {
            for (int x = -2; x <= 2; x++) {
                boolean dark = Math.max(Math.abs(y), Math.abs(x)) != 1;
                setFunction(modules, reserved, row + y, column + x, dark);
            }
        }
    }

    private static void placeData(byte[] codewords, boolean[][] modules, boolean[][] reserved) {
        BitReader reader = new BitReader(codewords);
        int size = modules.length;
        int direction = -1;
        int row = size - 1;
        for (int column = size - 1; column > 0; column -= 2) {
            if (column == 6) {
                column--;
            }
            while (row >= 0 && row < size) {
                for (int offset = 0; offset < 2; offset++) {
                    int c = column - offset;
                    if (!reserved[row][c]) {
                        modules[row][c] = reader.next();
                    }
                }
                row += direction;
            }
            row -= direction;
            direction = -direction;
        }
    }

    private static void applyMask(boolean[][] modules, boolean[][] reserved) {
        for (int row = 0; row < modules.length; row++) {
            for (int column = 0; column < modules.length; column++) {
                if (!reserved[row][column] && ((row + column) % 2 == 0)) {
                    modules[row][column] = !modules[row][column];
                }
            }
        }
    }

    private static void drawFormatBits(boolean[][] modules, boolean[][] reserved) {
        int format = 0b111011111000100; // Error correction L, mask 0.
        int size = modules.length;
        for (int index = 0; index < 15; index++) {
            boolean bit = ((format >>> index) & 1) != 0;
            int[] first = formatPositionOne(index);
            setFunction(modules, reserved, first[0], first[1], bit);
            int[] second = formatPositionTwo(size, index);
            setFunction(modules, reserved, second[0], second[1], bit);
        }
    }

    private static int[] formatPositionOne(int index) {
        if (index <= 5) {
            return new int[] {8, index};
        }
        if (index == 6) {
            return new int[] {8, 7};
        }
        if (index == 7) {
            return new int[] {8, 8};
        }
        if (index == 8) {
            return new int[] {7, 8};
        }
        return new int[] {14 - index, 8};
    }

    private static int[] formatPositionTwo(int size, int index) {
        if (index <= 7) {
            return new int[] {size - 1 - index, 8};
        }
        return new int[] {8, size - 15 + index};
    }

    private static void reserveFormatAreas(boolean[][] reserved) {
        int size = reserved.length;
        for (int index = 0; index < 9; index++) {
            reserved[8][index] = true;
            reserved[index][8] = true;
            reserved[8][size - 1 - index] = true;
            reserved[size - 1 - index][8] = true;
        }
    }

    private static boolean isFinderOverlap(int size, int row, int column) {
        return row <= 8 && column <= 8 || row <= 8 && column >= size - 9 || row >= size - 9 && column <= 8;
    }

    private static void setFunction(boolean[][] modules, boolean[][] reserved, int row, int column, boolean dark) {
        modules[row][column] = dark;
        reserved[row][column] = true;
    }

    private static int gfPow(int exponent) {
        int value = 1;
        for (int index = 0; index < exponent; index++) {
            value = gfMultiply(value, 2);
        }
        return value;
    }

    private static int gfMultiply(int left, int right) {
        int result = 0;
        int value = left;
        int factor = right;
        while (factor != 0) {
            if ((factor & 1) != 0) {
                result ^= value;
            }
            value <<= 1;
            if ((value & 0x100) != 0) {
                value ^= 0x11D;
            }
            factor >>>= 1;
        }
        return result & 0xFF;
    }

    private static boolean[][] copy(boolean[][] source) {
        boolean[][] copy = new boolean[source.length][source.length];
        for (int row = 0; row < source.length; row++) {
            System.arraycopy(source[row], 0, copy[row], 0, source[row].length);
        }
        return copy;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class BitBuffer {
        private boolean[] bits = new boolean[256];
        private int size;

        void append(int value, int bitCount) {
            ensure(size + bitCount);
            for (int index = bitCount - 1; index >= 0; index--) {
                bits[size++] = ((value >>> index) & 1) != 0;
            }
        }

        int size() {
            return size;
        }

        int toBytes(byte[] output) {
            int byteCount = size / 8;
            for (int index = 0; index < byteCount; index++) {
                int value = 0;
                for (int bit = 0; bit < 8; bit++) {
                    value = (value << 1) | (bits[index * 8 + bit] ? 1 : 0);
                }
                output[index] = (byte) value;
            }
            return byteCount;
        }

        private void ensure(int capacity) {
            if (capacity > bits.length) {
                bits = Arrays.copyOf(bits, Math.max(capacity, bits.length * 2));
            }
        }
    }

    private static final class BitReader {
        private final byte[] bytes;
        private int bitIndex;

        BitReader(byte[] bytes) {
            this.bytes = bytes;
        }

        boolean next() {
            if (bitIndex >= bytes.length * 8) {
                return false;
            }
            boolean value = ((bytes[bitIndex / 8] >>> (7 - bitIndex % 8)) & 1) != 0;
            bitIndex++;
            return value;
        }
    }
}
