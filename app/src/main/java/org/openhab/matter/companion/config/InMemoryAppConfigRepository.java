package org.openhab.matter.companion.config;

public final class InMemoryAppConfigRepository implements AppConfigRepository {
    private AppConfig config = new AppConfig("", "");

    @Override
    public AppConfig load() {
        return config;
    }

    @Override
    public void save(AppConfig config) {
        this.config = config;
    }
}
