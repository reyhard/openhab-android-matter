package org.openhab.matter.companion.otbr;

public final class OtbrStatus {
    private final boolean reachable;
    private final String message;
    private final String details;

    public OtbrStatus(boolean reachable, String message, String details) {
        this.reachable = reachable;
        this.message = message == null ? "" : message;
        this.details = details == null ? "" : details;
    }

    public boolean reachable() {
        return reachable;
    }

    public String message() {
        return message;
    }

    public String details() {
        return details;
    }
}
