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
            return new MatterSetupPayload(raw, 0L, -1, "Unknown vendor", "Unknown product", true);
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
        if (pin < 1 || pin > 99999999L) {
            throw new IllegalArgumentException("PIN must be an 8-digit Matter setup PIN.");
        }
        if (discriminator < 0 || discriminator > 4095) {
            throw new IllegalArgumentException("Long discriminator must be between 0 and 4095.");
        }
        return new MatterSetupPayload(raw, pin, discriminator, vendor, product, false);
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
}
