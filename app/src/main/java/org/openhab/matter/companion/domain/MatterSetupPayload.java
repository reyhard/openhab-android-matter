package org.openhab.matter.companion.domain;

public final class MatterSetupPayload {
    private final String rawPayload;
    private final long pin;
    private final int discriminator;
    private final String vendorName;
    private final String productName;
    private final boolean requiresChipParser;

    public MatterSetupPayload(String rawPayload, long pin, int discriminator, String vendorName, String productName, boolean requiresChipParser) {
        this.rawPayload = rawPayload;
        this.pin = pin;
        this.discriminator = discriminator;
        this.vendorName = vendorName;
        this.productName = productName;
        this.requiresChipParser = requiresChipParser;
    }

    public String rawPayload() {
        return rawPayload;
    }

    public long pin() {
        return pin;
    }

    public int discriminator() {
        return discriminator;
    }

    public String vendorName() {
        return vendorName;
    }

    public String productName() {
        return productName;
    }

    public boolean requiresChipParser() {
        return requiresChipParser;
    }
}
