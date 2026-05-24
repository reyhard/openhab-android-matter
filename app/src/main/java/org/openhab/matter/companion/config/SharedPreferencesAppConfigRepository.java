package org.openhab.matter.companion.config;

import android.content.Context;
import android.content.SharedPreferences;

public final class SharedPreferencesAppConfigRepository implements AppConfigRepository {
    private static final String PREFERENCE_FILE = "openhab_matter_config";
    private static final String KEY_THREAD_DATASET = "thread_dataset";
    private static final String KEY_OPENHAB_BASE_URL = "openhab_base_url";

    private final SharedPreferences preferences;
    private final SecureAppConfigMapper mapper;

    public SharedPreferencesAppConfigRepository(Context context) {
        this(context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE),
                new SecureAppConfigMapper(new AndroidKeystoreSecretCodec()));
    }

    SharedPreferencesAppConfigRepository(SharedPreferences preferences, SecureAppConfigMapper mapper) {
        this.preferences = preferences;
        this.mapper = mapper;
    }

    @Override
    public AppConfig load() {
        String storedThreadDataset = preferences.getString(KEY_THREAD_DATASET, "");
        String storedOpenHabBaseUrl = preferences.getString(KEY_OPENHAB_BASE_URL, "");
        AppConfig config = mapper.fromStoredValues(storedThreadDataset, storedOpenHabBaseUrl);
        if (mapper.isLegacyPlaintextThreadDataset(storedThreadDataset)) {
            try {
                save(config);
            } catch (IllegalStateException ignored) {
                // Loading legacy plaintext should not fail app startup if migration cannot be written.
            }
        }
        return config;
    }

    @Override
    public void save(AppConfig config) {
        try {
            SecureAppConfigMapper.StoredConfig storedConfig = mapper.toStoredValues(config);
            preferences.edit()
                    .putString(KEY_THREAD_DATASET, storedConfig.threadDataset())
                    .putString(KEY_OPENHAB_BASE_URL, storedConfig.openHabBaseUrl())
                    .apply();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save app config", e);
        }
    }
}
