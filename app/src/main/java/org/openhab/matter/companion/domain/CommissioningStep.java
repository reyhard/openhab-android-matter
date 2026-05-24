package org.openhab.matter.companion.domain;

public final class CommissioningStep {
    private final String message;
    private final boolean complete;

    public CommissioningStep(String message, boolean complete) {
        this.message = message;
        this.complete = complete;
    }

    public String message() {
        return message;
    }

    public boolean complete() {
        return complete;
    }
}