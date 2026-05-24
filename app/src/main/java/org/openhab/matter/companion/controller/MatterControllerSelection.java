package org.openhab.matter.companion.controller;

public final class MatterControllerSelection {
    private final MatterController controller;
    private final boolean nativeSelected;
    private final String message;

    public MatterControllerSelection(MatterController controller, boolean nativeSelected, String message) {
        if (controller == null) {
            throw new IllegalArgumentException("Matter controller is required.");
        }
        this.controller = controller;
        this.nativeSelected = nativeSelected;
        this.message = message == null ? "" : message;
    }

    public MatterController controller() {
        return controller;
    }

    public boolean nativeSelected() {
        return nativeSelected;
    }

    public String message() {
        return message;
    }
}
