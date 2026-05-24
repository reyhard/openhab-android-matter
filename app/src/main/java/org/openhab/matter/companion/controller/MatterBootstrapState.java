package org.openhab.matter.companion.controller;

public final class MatterBootstrapState {
    private final long bootstrapNodeId;
    private final String controllerState;
    private final boolean stateUnreadable;

    public MatterBootstrapState(long bootstrapNodeId, String controllerState, boolean stateUnreadable) {
        this.bootstrapNodeId = bootstrapNodeId;
        this.controllerState = controllerState == null ? "" : controllerState;
        this.stateUnreadable = stateUnreadable;
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
}
