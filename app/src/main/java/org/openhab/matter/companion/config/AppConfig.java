package org.openhab.matter.companion.config;

public final class AppConfig {
    private final String threadDataset;
    private final String openHabBaseUrl;
    private final String openHabApiToken;
    private final String otbrBaseUrl;
    private final boolean threadDatasetUnreadable;
    private final boolean openHabApiTokenUnreadable;
    private final boolean attestationBypassEnabled;

    public AppConfig(String threadDataset, String openHabBaseUrl) {
        this(threadDataset, openHabBaseUrl, "", "", false, false, false);
    }

    public AppConfig(String threadDataset, String openHabBaseUrl, String otbrBaseUrl) {
        this(threadDataset, openHabBaseUrl, "", otbrBaseUrl, false, false, false);
    }

    public AppConfig(String threadDataset, String openHabBaseUrl, String openHabApiToken, String otbrBaseUrl) {
        this(threadDataset, openHabBaseUrl, openHabApiToken, otbrBaseUrl, false, false, false);
    }

    public AppConfig(String threadDataset, String openHabBaseUrl, boolean threadDatasetUnreadable) {
        this(threadDataset, openHabBaseUrl, "", "", threadDatasetUnreadable, false, false);
    }

    public AppConfig(String threadDataset, String openHabBaseUrl, String otbrBaseUrl, boolean threadDatasetUnreadable) {
        this(threadDataset, openHabBaseUrl, "", otbrBaseUrl, threadDatasetUnreadable, false, false);
    }

    public AppConfig(
            String threadDataset,
            String openHabBaseUrl,
            String otbrBaseUrl,
            boolean threadDatasetUnreadable,
            boolean attestationBypassEnabled) {
        this(threadDataset, openHabBaseUrl, "", otbrBaseUrl, threadDatasetUnreadable, false, attestationBypassEnabled);
    }

    public AppConfig(
            String threadDataset,
            String openHabBaseUrl,
            String openHabApiToken,
            String otbrBaseUrl,
            boolean threadDatasetUnreadable,
            boolean attestationBypassEnabled) {
        this(threadDataset, openHabBaseUrl, openHabApiToken, otbrBaseUrl, threadDatasetUnreadable, false,
                attestationBypassEnabled);
    }

    public AppConfig(
            String threadDataset,
            String openHabBaseUrl,
            String openHabApiToken,
            String otbrBaseUrl,
            boolean threadDatasetUnreadable,
            boolean openHabApiTokenUnreadable,
            boolean attestationBypassEnabled) {
        this.threadDataset = threadDataset == null ? "" : threadDataset;
        this.openHabBaseUrl = openHabBaseUrl == null ? "" : openHabBaseUrl;
        this.openHabApiToken = openHabApiToken == null ? "" : openHabApiToken;
        this.otbrBaseUrl = otbrBaseUrl == null ? "" : otbrBaseUrl;
        this.threadDatasetUnreadable = threadDatasetUnreadable;
        this.openHabApiTokenUnreadable = openHabApiTokenUnreadable;
        this.attestationBypassEnabled = attestationBypassEnabled;
    }

    public String threadDataset() {
        return threadDataset;
    }

    public String openHabBaseUrl() {
        return openHabBaseUrl;
    }

    public String openHabApiToken() {
        return openHabApiToken;
    }

    public String otbrBaseUrl() {
        return otbrBaseUrl;
    }

    public boolean threadDatasetUnreadable() {
        return threadDatasetUnreadable;
    }

    public boolean openHabApiTokenUnreadable() {
        return openHabApiTokenUnreadable;
    }

    public boolean attestationBypassEnabled() {
        return attestationBypassEnabled;
    }
}
