package org.openhab.matter.companion.domain;

public final class MatterSetupPayload {
    private final String rawPayload;
    private final long pin;
    private final int discriminator;
    private final int vendorId;
    private final int productId;
    private final int commissioningFlow;
    private final int discoveryCapabilities;
    private final String vendorName;
    private final String productName;
    private final boolean requiresChipParser;

    public MatterSetupPayload(String rawPayload, long pin, int discriminator, String vendorName, String productName, boolean requiresChipParser) {
        this(rawPayload, pin, discriminator, -1, -1, -1, -1, vendorName, productName, requiresChipParser);
    }

    public MatterSetupPayload(
            String rawPayload,
            long pin,
            int discriminator,
            int vendorId,
            int productId,
            int commissioningFlow,
            int discoveryCapabilities,
            String vendorName,
            String productName,
            boolean requiresChipParser) {
        this.rawPayload = rawPayload;
        this.pin = pin;
        this.discriminator = discriminator;
        this.vendorId = vendorId;
        this.productId = productId;
        this.commissioningFlow = commissioningFlow;
        this.discoveryCapabilities = discoveryCapabilities;
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

    public int vendorId() {
        return vendorId;
    }

    public int productId() {
        return productId;
    }

    public int commissioningFlow() {
        return commissioningFlow;
    }

    public int discoveryCapabilities() {
        return discoveryCapabilities;
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
