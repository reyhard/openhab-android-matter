package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpReflectionAttestationHandler implements ConnectedHomeIpAttestationHandler {
    private final ConnectedHomeIpReflectionCommandFactory commandFactory;
    private final int failSafeExpiryTimeoutSeconds;
    private final ConnectedHomeIpAttestationTrustStore trustStore;

    public ConnectedHomeIpReflectionAttestationHandler(
            ConnectedHomeIpReflectionCommandFactory commandFactory,
            int failSafeExpiryTimeoutSeconds,
            ConnectedHomeIpAttestationTrustStore trustStore) {
        if (commandFactory == null) {
            throw new IllegalArgumentException("commandFactory is required");
        }
        if (failSafeExpiryTimeoutSeconds < 0) {
            throw new IllegalArgumentException("failSafeExpiryTimeoutSeconds must not be negative");
        }
        if (trustStore == null) {
            throw new IllegalArgumentException("trustStore is required");
        }
        this.commandFactory = commandFactory;
        this.failSafeExpiryTimeoutSeconds = failSafeExpiryTimeoutSeconds;
        this.trustStore = trustStore;
    }

    @Override
    public void prepareForCommissioning(Object controller, long nodeId, boolean attestationBypassEnabled) throws Exception {
        Object trustStoreDelegate = commandFactory.newAttestationTrustStoreDelegate(trustStore);
        commandFactory.invokeSetAttestationTrustStoreDelegate(
                controller,
                trustStoreDelegate,
                trustStore.cdTrustKeys());
        Object delegate = commandFactory.newDeviceAttestationDelegate(controller, attestationBypassEnabled);
        commandFactory.invokeSetDeviceAttestationDelegate(controller, failSafeExpiryTimeoutSeconds, delegate);
    }
}
