package org.openhab.matter.companion.config;

public interface AppConfigRepository {
    AppConfig load();

    void save(AppConfig config);
}
