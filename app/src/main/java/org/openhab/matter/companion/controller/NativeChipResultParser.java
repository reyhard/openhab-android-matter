package org.openhab.matter.companion.controller;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeChipResultParser {
    private NativeChipResultParser() {
    }

    public static NativeCommissioningResult parseCommissioningResult(String rawResult) {
        Map<String, String> values = parseKeyValueResult(rawResult);
        String nodeIdValue = values.get("nodeId");
        if (nodeIdValue == null || nodeIdValue.trim().isEmpty()) {
            throw new IllegalStateException("Native CHIP commissioning result is missing nodeId");
        }
        try {
            return new NativeCommissioningResult(
                    Long.parseLong(nodeIdValue.trim()),
                    values.get("controllerState"));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Native CHIP commissioning result has invalid nodeId", e);
        }
    }

    public static NativeOpenCommissioningWindowResult parseOpenCommissioningWindowResult(String rawResult) {
        Map<String, String> values = parseKeyValueResult(rawResult);
        String temporaryCode = values.get("temporaryCode");
        if (temporaryCode == null || temporaryCode.trim().isEmpty()) {
            throw new IllegalStateException("Native CHIP OpenCommissioningWindow result is missing temporaryCode");
        }
        return new NativeOpenCommissioningWindowResult(temporaryCode, values.get("controllerState"));
    }

    private static Map<String, String> parseKeyValueResult(String rawResult) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rawResult == null || rawResult.trim().isEmpty()) {
            return values;
        }
        String[] parts = rawResult.split(";");
        for (String part : parts) {
            int separator = part.indexOf('=');
            if (separator > 0) {
                String key = part.substring(0, separator).trim();
                String value = part.substring(separator + 1).trim();
                if (!key.isEmpty()) {
                    values.put(key, value);
                }
            }
        }
        return values;
    }
}
