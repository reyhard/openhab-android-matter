package org.openhab.matter.companion.openhab;

public final class FakeOpenHabInboxClient implements OpenHabInboxClient {
    private static final String DETECTED_MESSAGE = "Matter Inbox entry detected";
    private static final String NOT_DETECTED_MESSAGE = "No Matter Inbox entry detected";

    private final boolean reachable;
    private final boolean matterEntryDetected;
    private final String message;
    private final String details;

    private FakeOpenHabInboxClient(boolean reachable, boolean matterEntryDetected, String message, String details) {
        this.reachable = reachable;
        this.matterEntryDetected = matterEntryDetected;
        this.message = message;
        this.details = details;
    }

    public static FakeOpenHabInboxClient matterDeviceDetected() {
        return new FakeOpenHabInboxClient(true, true, DETECTED_MESSAGE, null);
    }

    public static FakeOpenHabInboxClient noMatterDevice() {
        return new FakeOpenHabInboxClient(true, false, NOT_DETECTED_MESSAGE, null);
    }

    public static FakeOpenHabInboxClient unreachable(String message, String details) {
        return new FakeOpenHabInboxClient(false, false, message, details);
    }

    @Override
    public OpenHabInboxStatus checkInbox(String baseUrl, String apiToken) {
        String resolvedDetails = details != null ? details : "Simulated Inbox check for " + baseUrl;
        return new OpenHabInboxStatus(reachable, matterEntryDetected, message, resolvedDetails);
    }
}
