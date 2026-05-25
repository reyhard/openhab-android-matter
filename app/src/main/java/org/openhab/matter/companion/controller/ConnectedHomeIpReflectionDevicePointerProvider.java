package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpReflectionDevicePointerProvider implements ConnectedHomeIpDevicePointerProvider {
    private final ConnectedHomeIpReflectionCommandFactory commandFactory;
    private final long timeoutMillis;

    public ConnectedHomeIpReflectionDevicePointerProvider(
            ConnectedHomeIpReflectionCommandFactory commandFactory,
            long timeoutMillis) {
        if (commandFactory == null) {
            throw new IllegalArgumentException("commandFactory is required");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        this.commandFactory = commandFactory;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public ConnectedHomeIpDevicePointer acquire(Object controller, long nodeId) throws Exception {
        ConnectedHomeIpConnectedDeviceCallback callback = new ConnectedHomeIpConnectedDeviceCallback(
                devicePtr -> commandFactory.invokeReleaseConnectedDevicePointer(controller, devicePtr));
        Object proxy = commandFactory.newGetConnectedDeviceCallback(callback);
        commandFactory.invokeGetConnectedDevicePointer(controller, nodeId, proxy);
        long devicePtr = callback.awaitDevicePointer(nodeId, timeoutMillis);
        return new ConnectedHomeIpDevicePointer(
                devicePtr,
                () -> commandFactory.invokeReleaseConnectedDevicePointer(controller, devicePtr));
    }
}
