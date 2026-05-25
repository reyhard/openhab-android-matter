package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpReflectionAttestationHandler implements ConnectedHomeIpAttestationHandler {
    private final ConnectedHomeIpReflectionCommandFactory commandFactory;
    private final int failSafeExpiryTimeoutSeconds;

    public ConnectedHomeIpReflectionAttestationHandler(
            ConnectedHomeIpReflectionCommandFactory commandFactory,
            int failSafeExpiryTimeoutSeconds) {
        if (commandFactory == null) {
            throw new IllegalArgumentException("commandFactory is required");
        }
        if (failSafeExpiryTimeoutSeconds < 0) {
            throw new IllegalArgumentException("failSafeExpiryTimeoutSeconds must not be negative");
        }
        this.commandFactory = commandFactory;
        this.failSafeExpiryTimeoutSeconds = failSafeExpiryTimeoutSeconds;
    }

    @Override
    public void prepareForCommissioning(Object controller, long nodeId, boolean attestationBypassEnabled) throws Exception {
        Object delegate = commandFactory.newDeviceAttestationDelegate(controller, attestationBypassEnabled);
        commandFactory.invokeSetDeviceAttestationDelegate(controller, failSafeExpiryTimeoutSeconds, delegate);
    }
}
