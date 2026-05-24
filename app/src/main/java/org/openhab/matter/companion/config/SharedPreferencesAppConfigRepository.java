package org.openhab.matter.companion.config;

import android.content.Context;
import android.content.SharedPreferences;

public final class SharedPreferencesAppConfigRepository implements AppConfigRepository {
    private static final String PREFERENCE_FILE = "openhab_matter_config";
    private static final String KEY_THREAD_DATASET = "thread_dataset";
    private static final String KEY_OPENHAB_BASE_URL = "openhab_base_url";

    private final SharedPreferences preferences;

    public SharedPreferencesAppConfigRepository(Context context) {
        preferences = context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
    }

    @Override
    public AppConfig load() {
        return new AppConfig(
                preferences.getString(KEY_THREAD_DATASET, ""),
                preferences.getString(KEY_OPENHAB_BASE_URL, ""));
    }

    @Override
    public void save(AppConfig config) {
        preferences.edit()
                .putString(KEY_THREAD_DATASET, config.threadDataset())
                .putString(KEY_OPENHAB_BASE_URL, config.openHabBaseUrl())
                .apply();
    }
}
