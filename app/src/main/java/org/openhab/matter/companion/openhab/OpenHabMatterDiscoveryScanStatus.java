package org.openhab.matter.companion.openhab;

public final class OpenHabMatterDiscoveryScanStatus {
    private final boolean reachable;
    private final boolean started;
    private final String message;
    private final String details;
    private final int timeoutSeconds;

    public OpenHabMatterDiscoveryScanStatus(boolean reachable, boolean started, String message, String details,
            int timeoutSeconds) {
        this.reachable = reachable;
        this.started = started;
        this.message = message;
        this.details = details;
        this.timeoutSeconds = Math.max(0, timeoutSeconds);
    }

    public boolean reachable() {
        return reachable;
    }

    public boolean started() {
        return started;
    }

    public String message() {
        return message;
    }

    public String details() {
        return details;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }
}
