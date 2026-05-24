package org.openhab.matter.companion.domain;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MatterSetupPayloadParser {
    private MatterSetupPayloadParser() {
    }

    public static MatterSetupPayload parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Matter setup payload is required.");
        }
        String raw = input.trim();
        if (raw.startsWith("MT:")) {
            return parseQrPayload(raw);
        }
        if (raw.matches("\\d{11}")) {
            throw new IllegalArgumentException("Thread commissioning needs the long discriminator from QR or explicit disc= value.");
        }
        Map<String, String> fields = parseFields(raw);
        String pinText = fields.get("pin");
        if (pinText == null || !pinText.matches("\\d{8}")) {
            throw new IllegalArgumentException("PIN must be an 8-digit Matter setup PIN.");
        }
        long pin = Long.parseLong(pinText);
        int discriminator = (int) parseLongField(fields, "disc", "long discriminator");
        String vendor = fields.getOrDefault("vendor", "Unknown vendor");
        String product = fields.getOrDefault("product", "Unknown product");
        validateSetupPin(pin);
        if (discriminator < 0 || discriminator > 4095) {
            throw new IllegalArgumentException("Long discriminator must be between 0 and 4095.");
        }
        return new MatterSetupPayload(raw, pin, discriminator, vendor, product, false);
    }

    private static MatterSetupPayload parseQrPayload(String raw) {
        byte[] bytes = Base38.decode(raw.substring(3));
        if (bytes.length != 11) {
            throw new IllegalArgumentException("Unsupported Matter QR payload length.");
        }
        BitReader reader = new BitReader(bytes);
        int version = (int) reader.readUnsigned(3);
        int vendorId = (int) reader.readUnsigned(16);
        int productId = (int) reader.readUnsigned(16);
        int commissioningFlow = (int) reader.readUnsigned(2);
        int discoveryCapabilities = (int) reader.readUnsigned(8);
        int discriminator = (int) reader.readUnsigned(12);
        long pin = reader.readUnsigned(27);
        validateSetupPin(pin);
        int padding = (int) reader.readUnsigned(4);
        if (version != 0) {
            throw new IllegalArgumentException("Unsupported Matter QR payload version.");
        }
        if (padding != 0) {
            throw new IllegalArgumentException("Invalid Matter QR payload padding.");
        }
        return new MatterSetupPayload(
                raw,
                pin,
                discriminator,
                vendorId,
                productId,
                commissioningFlow,
                discoveryCapabilities,
                "Unknown vendor",
                "Unknown product",
                false);
    }

    private static Map<String, String> parseFields(String raw) {
        Map<String, String> fields = new HashMap<>();
        for (String part : raw.split(";")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2) {
                fields.put(pair[0].trim().toLowerCase(Locale.US), pair[1].trim());
            }
        }
        return fields;
    }

    private static long parseLongField(Map<String, String> fields, String key, String label) {
        String value = fields.get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing " + label + ".");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + ".", ex);
        }
    }

    private static void validateSetupPin(long pin) {
        if (pin < 1 || pin > 99999998L) {
            throw new IllegalArgumentException("PIN must be an 8-digit Matter setup PIN.");
        }
    }
}
