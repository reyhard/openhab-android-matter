package org.openhab.matter.companion.openhab;

public final class OpenHabInboxEvent {
    private final String topic;
    private final String rawData;
    private final boolean matterEntryDetected;

    public OpenHabInboxEvent(String topic, String rawData, boolean matterEntryDetected) {
        this.topic = topic == null ? "" : topic;
        this.rawData = rawData == null ? "" : rawData;
        this.matterEntryDetected = matterEntryDetected;
    }

    public String topic() {
        return topic;
    }

    public String rawData() {
        return rawData;
    }

    public boolean matterEntryDetected() {
        return matterEntryDetected;
    }
}
