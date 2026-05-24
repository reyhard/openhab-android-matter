package org.openhab.matter.companion.config;

public final class AppConfig {
    private final String threadDataset;
    private final String openHabBaseUrl;
    private final boolean threadDatasetUnreadable;

    public AppConfig(String threadDataset, String openHabBaseUrl) {
        this(threadDataset, openHabBaseUrl, false);
    }

    public AppConfig(String threadDataset, String openHabBaseUrl, boolean threadDatasetUnreadable) {
        this.threadDataset = threadDataset == null ? "" : threadDataset;
        this.openHabBaseUrl = openHabBaseUrl == null ? "" : openHabBaseUrl;
        this.threadDatasetUnreadable = threadDatasetUnreadable;
    }

    public String threadDataset() {
        return threadDataset;
    }

    public String openHabBaseUrl() {
        return openHabBaseUrl;
    }

    public boolean threadDatasetUnreadable() {
        return threadDatasetUnreadable;
    }
}
