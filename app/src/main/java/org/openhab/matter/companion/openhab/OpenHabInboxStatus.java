package org.openhab.matter.companion.openhab;

public final class OpenHabInboxStatus {
    private final boolean reachable;
    private final boolean matterEntryDetected;
    private final String message;
    private final String details;

    public OpenHabInboxStatus(boolean reachable, boolean matterEntryDetected, String message, String details) {
        this.reachable = reachable;
        this.matterEntryDetected = matterEntryDetected;
        this.message = message;
        this.details = details;
    }

    public boolean reachable() {
        return reachable;
    }

    public boolean matterEntryDetected() {
        return matterEntryDetected;
    }

    public String message() {
        return message;
    }

    public String details() {
        return details;
    }
}
