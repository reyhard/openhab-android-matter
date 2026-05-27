package org.openhab.matter.companion.controller;

public final class MatterControllerSelector {
    private MatterControllerSelector() {
    }

    public static MatterControllerSelection select(
            MatterController fallbackController,
            MatterControllerCandidate nativeController,
            boolean preferNative) {
        if (fallbackController == null) {
            throw new IllegalArgumentException("Fallback Matter controller is required.");
        }
        if (!preferNative) {
            return new MatterControllerSelection(
                    fallbackController,
                    false,
                    "Using simulated Matter controller. Native Matter controller was not requested.");
        }
        if (nativeController == null) {
            return new MatterControllerSelection(
                    fallbackController,
                    false,
                    "Native Matter controller not configured.");
        }
        ChipMatterControllerStatus status = nativeController.readiness();
        if (status.ready()) {
            if (nativeController instanceof ConnectedHomeIpRuntimePreflightChecker) {
                ConnectedHomeIpRuntimePreflightStatus runtimeStatus =
                        ((ConnectedHomeIpRuntimePreflightChecker) nativeController).checkRuntimePreflight();
                if (!runtimeStatus.ready()) {
                    return new MatterControllerSelection(
                            fallbackController,
                            false,
                            "Native Matter controller runtime preflight failed: " + runtimeStatus.message());
                }
            }
            return new MatterControllerSelection(
                    nativeController,
                    true,
                    "Using native Matter controller: " + status.libraryName());
        }
        return new MatterControllerSelection(
                fallbackController,
                false,
                "Native Matter controller not ready: " + status.message());
    }
}
