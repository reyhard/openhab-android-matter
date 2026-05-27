package org.openhab.matter.companion.controller;

import chip.devicecontroller.AttestationInfo;
import chip.devicecontroller.ChipDeviceController;
import chip.devicecontroller.DeviceAttestationDelegate;

public final class ConnectedHomeIpConcreteDeviceAttestationDelegate implements DeviceAttestationDelegate {
    private final ChipDeviceController controller;
    private final boolean attestationBypassEnabled;

    public ConnectedHomeIpConcreteDeviceAttestationDelegate(
            ChipDeviceController controller,
            boolean attestationBypassEnabled) {
        if (controller == null) {
            throw new IllegalArgumentException("controller is required");
        }
        this.controller = controller;
        this.attestationBypassEnabled = attestationBypassEnabled;
    }

    @Override
    public void onDeviceAttestationCompleted(long devicePtr, AttestationInfo attestationInfo, long errorCode) {
        ConnectedHomeIpDiagnostics.emit(
                "Device attestation completed with error "
                        + errorCode
                        + "; continuing commissioning with attestation bypass "
                        + (attestationBypassEnabled ? "enabled" : "disabled"));
        new Thread(() -> {
            try {
                controller.continueCommissioning(devicePtr, attestationBypassEnabled);
            } catch (RuntimeException exception) {
                ConnectedHomeIpDiagnostics.emit(
                        "Device attestation continuation failed: " + safeMessage(exception));
            }
        }, "matter-attestation-continuation").start();
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isEmpty() ? throwable.getClass().getSimpleName() : message;
    }
}
