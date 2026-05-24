package org.openhab.matter.companion.domain;

import java.util.Locale;

public final class ThreadDataset {
    private final String hex;

    private ThreadDataset(String hex) {
        this.hex = hex;
    }

    public static ThreadDataset parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Thread dataset is required.");
        }
        String normalized = input.trim().replaceAll("\\s+", "");
        if (normalized.toLowerCase(Locale.US).startsWith("hex:")) {
            normalized = normalized.substring(4);
        }
        normalized = normalized.toUpperCase(Locale.US);
        if (normalized.length() < 16) {
            throw new IllegalArgumentException("Thread dataset is too short.");
        }
        if ((normalized.length() % 2) != 0) {
            throw new IllegalArgumentException("Thread dataset hex must contain complete bytes.");
        }
        if (!normalized.matches("[0-9A-F]+")) {
            throw new IllegalArgumentException("Thread dataset must be hexadecimal.");
        }
        return new ThreadDataset(normalized);
    }

    public String hex() {
        return hex;
    }

    public String chipToolValue() {
        return "hex:" + hex;
    }
}
