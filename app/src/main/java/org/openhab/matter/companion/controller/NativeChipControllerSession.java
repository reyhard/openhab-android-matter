package org.openhab.matter.companion.controller;

public final class NativeChipControllerSession {
    private final MatterController fallbackController;
    private final NativeControllerFactory nativeControllerFactory;
    private ChipMatterController nativeController;
    private MatterController controller;
    private boolean nativeSelected;
    private boolean attestationBypassEnabled;
    private int generation;

    public NativeChipControllerSession(
            MatterController fallbackController,
            boolean attestationBypassEnabled,
            NativeControllerFactory nativeControllerFactory) {
        if (fallbackController == null) {
            throw new IllegalArgumentException("Fallback Matter controller is required.");
        }
        if (nativeControllerFactory == null) {
            throw new IllegalArgumentException("Native controller factory is required.");
        }
        this.fallbackController = fallbackController;
        this.nativeControllerFactory = nativeControllerFactory;
        this.attestationBypassEnabled = attestationBypassEnabled;
        this.nativeController = nativeControllerFactory.create(attestationBypassEnabled);
        this.controller = fallbackController;
    }

    public synchronized MatterController controller() {
        return controller;
    }

    public synchronized boolean nativeSelected() {
        return nativeSelected;
    }

    public synchronized boolean attestationBypassEnabled() {
        return attestationBypassEnabled;
    }

    public synchronized boolean syncAttestationBypass(boolean enabled) {
        if (attestationBypassEnabled == enabled) {
            return false;
        }
        attestationBypassEnabled = enabled;
        nativeController = nativeControllerFactory.create(enabled);
        controller = fallbackController;
        nativeSelected = false;
        generation++;
        return true;
    }

    public synchronized SelectionRequest selectionRequest() {
        return new SelectionRequest(generation, nativeController);
    }

    public synchronized boolean applySelection(SelectionRequest request, MatterControllerSelection selection) {
        if (request == null || selection == null || request.generation != generation
                || request.nativeController != nativeController) {
            return false;
        }
        controller = selection.controller();
        nativeSelected = selection.nativeSelected();
        return true;
    }

    public synchronized boolean isCurrent(SelectionRequest request) {
        return request != null && request.generation == generation && request.nativeController == nativeController;
    }

    public interface NativeControllerFactory {
        ChipMatterController create(boolean attestationBypassEnabled);
    }

    public static final class SelectionRequest {
        private final int generation;
        private final ChipMatterController nativeController;

        private SelectionRequest(int generation, ChipMatterController nativeController) {
            this.generation = generation;
            this.nativeController = nativeController;
        }

        public ChipMatterController nativeController() {
            return nativeController;
        }
    }
}
