package org.openhab.matter.companion.openhab;

public final class OpenHabStatus {
    private final boolean online;
    private final String message;
    private final String details;

    public OpenHabStatus(boolean online, String message, String details) {
        this.online = online;
        this.message = message;
        this.details = details;
    }

    public boolean online() {
        return online;
    }

    public String message() {
        return message;
    }

    public String details() {
        return details;
    }
}
