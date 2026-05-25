package org.openhab.matter.companion.controller;

public final class MatterBootstrapStateResolver {
    private MatterBootstrapStateResolver() {
    }

    public static long resolveNodeId(long savedInstanceNodeId, MatterBootstrapState persistedState) {
        if (persistedState != null && persistedState.stateUnreadable()) {
            return -1L;
        }
        if (persistedState != null && persistedState.bootstrapNodeId() >= 0) {
            return persistedState.bootstrapNodeId();
        }
        return savedInstanceNodeId;
    }
}
