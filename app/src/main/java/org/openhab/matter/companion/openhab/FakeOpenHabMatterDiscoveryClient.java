package org.openhab.matter.companion.openhab;

public final class FakeOpenHabMatterDiscoveryClient implements OpenHabMatterDiscoveryClient {
    private final boolean reachable;
    private final boolean started;
    private final String message;
    private final String details;
    private final int timeoutSeconds;

    private FakeOpenHabMatterDiscoveryClient(boolean reachable, boolean started, String message, String details,
            int timeoutSeconds) {
        this.reachable = reachable;
        this.started = started;
        this.message = message;
        this.details = details;
        this.timeoutSeconds = timeoutSeconds;
    }

    public static FakeOpenHabMatterDiscoveryClient started(int timeoutSeconds) {
        return new FakeOpenHabMatterDiscoveryClient(true, true, "openHAB Matter scan started", null, timeoutSeconds);
    }

    public static FakeOpenHabMatterDiscoveryClient failed(String message, String details) {
        return new FakeOpenHabMatterDiscoveryClient(false, false, message, details, 0);
    }

    @Override
    public OpenHabMatterDiscoveryScanStatus startMatterScan(String baseUrl, String pairingCode, String apiToken) {
        String resolvedDetails = details != null ? details : "Simulated Matter scan start for " + baseUrl;
        return new OpenHabMatterDiscoveryScanStatus(reachable, started, message, resolvedDetails, timeoutSeconds);
    }
}
