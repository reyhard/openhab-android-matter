package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpFabricRestoreProbe {
    private final ConnectedHomeIpControllerProvider controllerProvider;
    private final ConnectedHomeIpDevicePointerProvider devicePointerProvider;

    public ConnectedHomeIpFabricRestoreProbe(
            ConnectedHomeIpControllerProvider controllerProvider,
            ConnectedHomeIpDevicePointerProvider devicePointerProvider) {
        if (controllerProvider == null) {
            throw new IllegalArgumentException("controllerProvider is required");
        }
        if (devicePointerProvider == null) {
            throw new IllegalArgumentException("devicePointerProvider is required");
        }
        this.controllerProvider = controllerProvider;
        this.devicePointerProvider = devicePointerProvider;
    }

    public ConnectedHomeIpFabricRestoreStatus check(long bootstrapNodeId) {
        if (bootstrapNodeId < 0) {
            return new ConnectedHomeIpFabricRestoreStatus(
                    false,
                    false,
                    -1L,
                    "No connectedhomeip bootstrap fabric has been commissioned yet.");
        }
        try {
            Object controller = controllerProvider.controller();
            try (ConnectedHomeIpDevicePointer ignored = devicePointerProvider.acquire(controller, bootstrapNodeId)) {
                return new ConnectedHomeIpFabricRestoreStatus(
                        true,
                        true,
                        bootstrapNodeId,
                        "connectedhomeip fabric restore ready for node " + bootstrapNodeId + ".");
            }
        } catch (Exception | LinkageError ex) {
            return new ConnectedHomeIpFabricRestoreStatus(
                    true,
                    false,
                    bootstrapNodeId,
                    "connectedhomeip fabric restore is not ready for node "
                            + bootstrapNodeId
                            + ": "
                            + safeMessage(ex));
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isEmpty() ? throwable.getClass().getSimpleName() : message;
    }
}
