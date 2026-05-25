package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpAttestationHandler {
    void prepareForCommissioning(Object controller, long nodeId, boolean attestationBypassEnabled) throws Exception;
}
