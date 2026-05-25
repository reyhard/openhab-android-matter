package org.openhab.matter.companion.controller;

public class ConnectedHomeIpDevicePointer implements AutoCloseable {
    private final long value;
    private final AutoCloseable releaseAction;

    public ConnectedHomeIpDevicePointer(long value, AutoCloseable releaseAction) {
        this.value = value;
        this.releaseAction = requireReleaseAction(releaseAction);
    }

    public long value() {
        return value;
    }

    @Override
    public void close() throws Exception {
        releaseAction.close();
    }

    private static AutoCloseable requireReleaseAction(AutoCloseable releaseAction) {
        if (releaseAction == null) {
            throw new IllegalArgumentException("releaseAction is required");
        }
        return releaseAction;
    }
}
