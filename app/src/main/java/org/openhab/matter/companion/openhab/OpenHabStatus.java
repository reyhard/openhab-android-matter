package org.openhab.matter.companion.openhab;

public final class OpenHabStatus {
    private final boolean online;
    private final boolean restReachable;
    private final boolean matterControllerReady;
    private final String message;
    private final String details;

    public OpenHabStatus(boolean online, String message, String details) {
        this(online, online, online, message, details);
    }

    public OpenHabStatus(boolean online, boolean restReachable, boolean matterControllerReady, String message, String details) {
        this.online = online;
        this.restReachable = restReachable;
        this.matterControllerReady = matterControllerReady;
        this.message = message;
        this.details = details;
    }

    public boolean online() {
        return online;
    }

    public boolean restReachable() {
        return restReachable;
    }

    public boolean matterControllerReady() {
        return matterControllerReady;
    }

    public String message() {
        return message;
    }

    public String details() {
        return details;
    }
}
