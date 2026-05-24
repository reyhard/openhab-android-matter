package org.openhab.matter.companion.controller;

public final class ChipMatterControllerConfig {
    private static final String DEFAULT_LIBRARY = "openhab_matter_chip";

    private final String nativeLibraryName;
    private final boolean attestationBypassEnabled;

    public ChipMatterControllerConfig(String nativeLibraryName, boolean attestationBypassEnabled) {
        this.nativeLibraryName = nativeLibraryName == null || nativeLibraryName.trim().isEmpty()
                ? DEFAULT_LIBRARY : nativeLibraryName.trim();
        this.attestationBypassEnabled = attestationBypassEnabled;
    }

    public static ChipMatterControllerConfig defaultConfig() {
        return new ChipMatterControllerConfig(DEFAULT_LIBRARY, false);
    }

    public String nativeLibraryName() {
        return nativeLibraryName;
    }

    public boolean attestationBypassEnabled() {
        return attestationBypassEnabled;
    }
}
