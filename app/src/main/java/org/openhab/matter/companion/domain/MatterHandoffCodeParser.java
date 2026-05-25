package org.openhab.matter.companion.domain;

public final class MatterHandoffCodeParser {
    private static final int[] VERHOEFF_MULTIPLY_TABLE = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            1, 2, 3, 4, 0, 6, 7, 8, 9, 5,
            2, 3, 4, 0, 1, 7, 8, 9, 5, 6,
            3, 4, 0, 1, 2, 8, 9, 5, 6, 7,
            4, 0, 1, 2, 3, 9, 5, 6, 7, 8,
            5, 9, 8, 7, 6, 0, 4, 3, 2, 1,
            6, 5, 9, 8, 7, 1, 0, 4, 3, 2,
            7, 6, 5, 9, 8, 2, 1, 0, 4, 3,
            8, 7, 6, 5, 9, 3, 2, 1, 0, 4,
            9, 8, 7, 6, 5, 4, 3, 2, 1, 0
    };
    private static final int[] VERHOEFF_PERM_TABLE = {1, 5, 7, 6, 2, 8, 3, 0, 9, 4};

    private MatterHandoffCodeParser() {
    }

    public static String parseForOpenHabScanInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Matter handoff code is required.");
        }

        String raw = input.trim();
        if (raw.startsWith("MT:")) {
            validateQrPayloadCore(raw);
            return raw;
        }

        String normalized = raw.replaceAll("[\\s-]", "");
        if (isValidManualCode(normalized)) {
            return normalized;
        }

        throw new IllegalArgumentException(
                "Enter a Matter QR payload or 11- or 21-digit manual setup/multi-admin code for openHAB Scan Input.");
    }

    private static void validateQrPayloadCore(String raw) {
        byte[] bytes = Base38.decode(raw.substring(3));
        if (bytes.length < 11) {
            throw new IllegalArgumentException("Unsupported Matter QR payload length.");
        }
        BitReader reader = new BitReader(bytes);
        int version = (int) reader.readUnsigned(3);
        reader.readUnsigned(16);
        reader.readUnsigned(16);
        int commissioningFlow = (int) reader.readUnsigned(2);
        int discoveryCapabilities = (int) reader.readUnsigned(8);
        reader.readUnsigned(12);
        long pin = reader.readUnsigned(27);
        int padding = (int) reader.readUnsigned(4);
        if (version != 0) {
            throw new IllegalArgumentException("Unsupported Matter QR payload version.");
        }
        if (commissioningFlow < 0 || commissioningFlow > 2) {
            throw new IllegalArgumentException("Unsupported Matter QR commissioning flow.");
        }
        if (discoveryCapabilities == 0) {
            throw new IllegalArgumentException("Matter QR payload discovery capabilities are required.");
        }
        if (!isValidSetupPin(pin)) {
            throw new IllegalArgumentException("PIN must be an 8-digit Matter setup PIN.");
        }
        if (padding != 0) {
            throw new IllegalArgumentException("Invalid Matter QR payload padding.");
        }
    }

    private static boolean isValidManualCode(String code) {
        if (!code.matches("\\d{11}|\\d{21}") || !hasValidCheckDigit(code)) {
            return false;
        }

        String body = code.substring(0, code.length() - 1);
        int chunk1 = parseInt(body, 0, 1);
        if (chunk1 == 8 || chunk1 == 9) {
            return false;
        }

        boolean isLongCode = ((chunk1 >> 2) & 1) == 1;
        if (body.length() != (isLongCode ? 20 : 10)) {
            return false;
        }

        int chunk2 = parseInt(body, 1, 6);
        int chunk3 = parseInt(body, 6, 10);
        long pin = (chunk2 & ((1 << 14) - 1))
                | ((long) (chunk3 & ((1 << 13) - 1)) << 14);
        if (!isValidSetupPin(pin)) {
            return false;
        }
        if (!isLongCode) {
            return true;
        }

        int vendorId = parseInt(body, 10, 15);
        int productId = parseInt(body, 15, 20);
        return isValidVendorId(vendorId) && isValidProductId(vendorId, productId);
    }

    private static int parseInt(String code, int start, int end) {
        return Integer.parseInt(code.substring(start, end));
    }

    private static boolean hasValidCheckDigit(String code) {
        char expected = computeCheckDigit(code.substring(0, code.length() - 1));
        return expected != 0 && expected == code.charAt(code.length() - 1);
    }

    private static char computeCheckDigit(String body) {
        int c = 0;
        for (int i = 1; i <= body.length(); i++) {
            int val = Character.digit(body.charAt(body.length() - i), 10);
            if (val < 0) {
                return 0;
            }
            int p = permute(val, i);
            c = VERHOEFF_MULTIPLY_TABLE[c * 10 + p];
        }
        c = dihedralInvert(c);
        return (char) ('0' + c);
    }

    private static int permute(int val, int iterations) {
        int result = val;
        for (int i = 0; i < iterations; i++) {
            result = VERHOEFF_PERM_TABLE[result];
        }
        return result;
    }

    private static int dihedralInvert(int val) {
        return val > 0 && val < 5 ? 5 - val : val;
    }

    private static boolean isValidSetupPin(long pin) {
        return pin >= 1
                && pin <= 99999998L
                && pin != 11111111L
                && pin != 22222222L
                && pin != 33333333L
                && pin != 44444444L
                && pin != 55555555L
                && pin != 66666666L
                && pin != 77777777L
                && pin != 88888888L
                && pin != 12345678L
                && pin != 87654321L;
    }

    private static boolean isValidVendorId(int vendorId) {
        return vendorId == 0 || (vendorId >= 1 && vendorId <= 65534);
    }

    private static boolean isValidProductId(int vendorId, int productId) {
        if (productId == 0) {
            return vendorId == 0;
        }
        return productId <= 65535;
    }
}
