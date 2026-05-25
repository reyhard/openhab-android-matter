package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpFabricRestoreStatus {
    private final boolean checked;
    private final boolean ready;
    private final long nodeId;
    private final String message;

    public ConnectedHomeIpFabricRestoreStatus(boolean checked, boolean ready, long nodeId, String message) {
        this.checked = checked;
        this.ready = ready;
        this.nodeId = nodeId;
        this.message = message == null ? "" : message;
    }

    public boolean checked() {
        return checked;
    }

    public boolean ready() {
        return ready;
    }

    public long nodeId() {
        return nodeId;
    }

    public String message() {
        return message;
    }
}
