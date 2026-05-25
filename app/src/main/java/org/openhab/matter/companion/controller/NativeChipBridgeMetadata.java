package org.openhab.matter.companion.controller;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeChipBridgeMetadata {
    private final String kind;
    private final String version;
    private final boolean production;
    private final String message;

    private NativeChipBridgeMetadata(String kind, String version, boolean production, String message) {
        this.kind = kind == null || kind.isEmpty() ? "unknown" : kind;
        this.version = version == null ? "" : version;
        this.production = production;
        this.message = message == null ? "" : message;
    }

    public static NativeChipBridgeMetadata parse(String rawMetadata) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rawMetadata != null) {
            String[] parts = rawMetadata.split(";");
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
        }
        return new NativeChipBridgeMetadata(
                values.get("kind"),
                values.get("version"),
                Boolean.parseBoolean(values.get("production")),
                values.get("message"));
    }

    public boolean productionReady() {
        return production && "connectedhomeip".equals(kind);
    }

    public String kind() {
        return kind;
    }

    public String version() {
        return version;
    }

    public String message() {
        return message;
    }
}
