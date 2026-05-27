package org.openhab.matter.companion.controller;

public final class NativeChipControllerSession {
    private final MatterController fallbackController;
    private final NativeControllerFactory nativeControllerFactory;
    private MatterControllerCandidate nativeController;
    private MatterController controller;
    private boolean nativeSelected;
    private boolean attestationBypassEnabled;
    private int generation;
    private int requestSequence;

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
        nativeController = null;
        controller = fallbackController;
        nativeSelected = false;
        generation++;
        return true;
    }

    public synchronized SelectionRequest selectionRequest() {
        if (nativeController == null) {
            nativeController = nativeControllerFactory.create(attestationBypassEnabled);
        }
        requestSequence++;
        return new SelectionRequest(generation, requestSequence, nativeController);
    }

    public synchronized boolean applySelection(SelectionRequest request, MatterControllerSelection selection) {
        if (request == null || selection == null || request.generation != generation
                || request.requestSequence != requestSequence
                || request.nativeController != nativeController) {
            return false;
        }
        controller = selection.controller();
        nativeSelected = selection.nativeSelected();
        return true;
    }

    public MatterControllerSelection selectNativeIfReady() {
        SelectionRequest request = selectionRequest();
        MatterControllerSelection selection = MatterControllerSelector.select(
                fallbackController,
                request.nativeController(),
                true);
        applySelection(request, selection);
        return selection;
    }

    public synchronized boolean isCurrent(SelectionRequest request) {
        return request != null
                && request.generation == generation
                && request.requestSequence == requestSequence
                && request.nativeController == nativeController;
    }

    public interface NativeControllerFactory {
        MatterControllerCandidate create(boolean attestationBypassEnabled);
    }

    public static final class SelectionRequest {
        private final int generation;
        private final int requestSequence;
        private final MatterControllerCandidate nativeController;

        private SelectionRequest(int generation, int requestSequence, MatterControllerCandidate nativeController) {
            this.generation = generation;
            this.requestSequence = requestSequence;
            this.nativeController = nativeController;
        }

        public MatterControllerCandidate nativeController() {
            return nativeController;
        }
    }
}
