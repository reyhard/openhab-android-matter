package org.openhab.matter.companion.controller;

public final class MatterBootstrapState {
    private final long bootstrapNodeId;
    private final String controllerState;
    private final boolean stateUnreadable;
    private final String vendorName;
    private final String productName;

    public MatterBootstrapState(long bootstrapNodeId, String controllerState, boolean stateUnreadable) {
        this(bootstrapNodeId, controllerState, stateUnreadable, "", "");
    }

    public MatterBootstrapState(
            long bootstrapNodeId,
            String controllerState,
            boolean stateUnreadable,
            String vendorName,
            String productName) {
        this.bootstrapNodeId = bootstrapNodeId;
        this.controllerState = controllerState == null ? "" : controllerState;
        this.stateUnreadable = stateUnreadable;
        this.vendorName = nullToEmpty(vendorName);
        this.productName = nullToEmpty(productName);
    }

    public static MatterBootstrapState empty() {
        return new MatterBootstrapState(-1L, "", false);
    }

    public long bootstrapNodeId() {
        return bootstrapNodeId;
    }

    public String controllerState() {
        return controllerState;
    }

    public boolean stateUnreadable() {
        return stateUnreadable;
    }

    public String vendorName() {
        return vendorName;
    }

    public String productName() {
        return productName;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
