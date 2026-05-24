package org.openhab.matter.companion.openhab;

public final class FakeOpenHabClient implements OpenHabClient {
    private static final String ONLINE_MESSAGE = "openHAB REST API is reachable";

    private final boolean online;
    private final String message;
    private final String details;

    private FakeOpenHabClient(boolean online, String message, String details) {
        this.online = online;
        this.message = message;
        this.details = details;
    }

    public static FakeOpenHabClient online() {
        return new FakeOpenHabClient(true, ONLINE_MESSAGE, null);
    }

    public static FakeOpenHabClient offline(String message, String details) {
        return new FakeOpenHabClient(false, message, details);
    }

    @Override
    public OpenHabStatus checkReadiness(String baseUrl) {
        String resolvedDetails = details != null ? details : "Simulated readiness check for " + baseUrl;
        return new OpenHabStatus(online, message, resolvedDetails);
    }
}
