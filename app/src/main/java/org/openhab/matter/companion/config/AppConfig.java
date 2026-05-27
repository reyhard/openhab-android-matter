package org.openhab.matter.companion.config;

public final class AppConfig {
    private final String threadDataset;
    private final String setupPayload;
    private final String openHabBaseUrl;
    private final String openHabApiToken;
    private final String otbrBaseUrl;
    private final boolean threadDatasetUnreadable;
    private final boolean setupPayloadUnreadable;
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

    public AppConfig(
            String threadDataset,
            String setupPayload,
            String openHabBaseUrl,
            String openHabApiToken,
            String otbrBaseUrl) {
        this(threadDataset, setupPayload, openHabBaseUrl, openHabApiToken, otbrBaseUrl, false, false, false, false);
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
        this(threadDataset, "", openHabBaseUrl, "", otbrBaseUrl, threadDatasetUnreadable, false, false,
                attestationBypassEnabled);
    }

    public AppConfig(
            String threadDataset,
            String openHabBaseUrl,
            String openHabApiToken,
            String otbrBaseUrl,
            boolean threadDatasetUnreadable,
            boolean attestationBypassEnabled) {
        this(threadDataset, "", openHabBaseUrl, openHabApiToken, otbrBaseUrl, threadDatasetUnreadable, false, false,
                attestationBypassEnabled);
    }

    public AppConfig(
            String threadDataset,
            String setupPayload,
            String openHabBaseUrl,
            String openHabApiToken,
            String otbrBaseUrl,
            boolean threadDatasetUnreadable,
            boolean attestationBypassEnabled) {
        this(threadDataset, setupPayload, openHabBaseUrl, openHabApiToken, otbrBaseUrl, threadDatasetUnreadable, false,
                false, attestationBypassEnabled);
    }

    public AppConfig(
            String threadDataset,
            String openHabBaseUrl,
            String openHabApiToken,
            String otbrBaseUrl,
            boolean threadDatasetUnreadable,
            boolean openHabApiTokenUnreadable,
            boolean attestationBypassEnabled) {
        this(threadDataset, "", openHabBaseUrl, openHabApiToken, otbrBaseUrl, threadDatasetUnreadable, false,
                openHabApiTokenUnreadable, attestationBypassEnabled);
    }

    public AppConfig(
            String threadDataset,
            String setupPayload,
            String openHabBaseUrl,
            String openHabApiToken,
            String otbrBaseUrl,
            boolean threadDatasetUnreadable,
            boolean setupPayloadUnreadable,
            boolean openHabApiTokenUnreadable,
            boolean attestationBypassEnabled) {
        this.threadDataset = threadDataset == null ? "" : threadDataset;
        this.setupPayload = setupPayload == null ? "" : setupPayload;
        this.openHabBaseUrl = openHabBaseUrl == null ? "" : openHabBaseUrl;
        this.openHabApiToken = openHabApiToken == null ? "" : openHabApiToken;
        this.otbrBaseUrl = otbrBaseUrl == null ? "" : otbrBaseUrl;
        this.threadDatasetUnreadable = threadDatasetUnreadable;
        this.setupPayloadUnreadable = setupPayloadUnreadable;
        this.openHabApiTokenUnreadable = openHabApiTokenUnreadable;
        this.attestationBypassEnabled = attestationBypassEnabled;
    }

    public String threadDataset() {
        return threadDataset;
    }

    public String setupPayload() {
        return setupPayload;
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

    public boolean setupPayloadUnreadable() {
        return setupPayloadUnreadable;
    }

    public boolean openHabApiTokenUnreadable() {
        return openHabApiTokenUnreadable;
    }

    public boolean attestationBypassEnabled() {
        return attestationBypassEnabled;
    }
}
