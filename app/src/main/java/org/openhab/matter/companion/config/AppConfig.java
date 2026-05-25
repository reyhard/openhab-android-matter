package org.openhab.matter.companion.config;

public final class AppConfig {
    private final String threadDataset;
    private final String openHabBaseUrl;
    private final String otbrBaseUrl;
    private final boolean threadDatasetUnreadable;
    private final boolean attestationBypassEnabled;

    public AppConfig(String threadDataset, String openHabBaseUrl) {
        this(threadDataset, openHabBaseUrl, "", false);
    }

    public AppConfig(String threadDataset, String openHabBaseUrl, String otbrBaseUrl) {
        this(threadDataset, openHabBaseUrl, otbrBaseUrl, false);
    }

    public AppConfig(String threadDataset, String openHabBaseUrl, boolean threadDatasetUnreadable) {
        this(threadDataset, openHabBaseUrl, "", threadDatasetUnreadable);
    }

    public AppConfig(String threadDataset, String openHabBaseUrl, String otbrBaseUrl, boolean threadDatasetUnreadable) {
        this(threadDataset, openHabBaseUrl, otbrBaseUrl, threadDatasetUnreadable, false);
    }

    public AppConfig(
            String threadDataset,
            String openHabBaseUrl,
            String otbrBaseUrl,
            boolean threadDatasetUnreadable,
            boolean attestationBypassEnabled) {
        this.threadDataset = threadDataset == null ? "" : threadDataset;
        this.openHabBaseUrl = openHabBaseUrl == null ? "" : openHabBaseUrl;
        this.otbrBaseUrl = otbrBaseUrl == null ? "" : otbrBaseUrl;
        this.threadDatasetUnreadable = threadDatasetUnreadable;
        this.attestationBypassEnabled = attestationBypassEnabled;
    }

    public String threadDataset() {
        return threadDataset;
    }

    public String openHabBaseUrl() {
        return openHabBaseUrl;
    }

    public String otbrBaseUrl() {
        return otbrBaseUrl;
    }

    public boolean threadDatasetUnreadable() {
        return threadDatasetUnreadable;
    }

    public boolean attestationBypassEnabled() {
        return attestationBypassEnabled;
    }
}
