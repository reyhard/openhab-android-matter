package org.openhab.matter.companion.controller;

public final class MatterControllerSelector {
    private MatterControllerSelector() {
    }

    public static MatterControllerSelection select(
            MatterController fallbackController,
            ChipMatterController nativeController,
            boolean preferNative) {
        if (fallbackController == null) {
            throw new IllegalArgumentException("Fallback Matter controller is required.");
        }
        if (!preferNative) {
            return new MatterControllerSelection(
                    fallbackController,
                    false,
                    "Using simulated Matter controller. Native CHIP controller was not requested.");
        }
        if (nativeController == null) {
            return new MatterControllerSelection(
                    fallbackController,
                    false,
                    "Native CHIP controller not configured. Continuing with simulated Matter controller.");
        }
        ChipMatterControllerStatus status = nativeController.readiness();
        if (status.ready()) {
            return new MatterControllerSelection(
                    nativeController,
                    true,
                    "Using native CHIP controller: " + status.libraryName());
        }
        return new MatterControllerSelection(
                fallbackController,
                false,
                "Native CHIP controller not ready: " + status.message()
                        + ". Continuing with simulated Matter controller.");
    }
}
