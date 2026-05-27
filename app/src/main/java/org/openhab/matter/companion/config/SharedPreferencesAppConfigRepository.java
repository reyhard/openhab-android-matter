package org.openhab.matter.companion.config;

import android.content.Context;
import android.content.SharedPreferences;

public final class SharedPreferencesAppConfigRepository implements AppConfigRepository {
    private static final String PREFERENCE_FILE = "openhab_matter_config";
    private static final String KEY_THREAD_DATASET = "thread_dataset";
    private static final String KEY_SETUP_PAYLOAD = "setup_payload";
    private static final String KEY_OPENHAB_BASE_URL = "openhab_base_url";
    private static final String KEY_OPENHAB_API_TOKEN = "openhab_api_token";
    private static final String KEY_OTBR_BASE_URL = "otbr_base_url";
    private static final String KEY_ATTESTATION_BYPASS_ENABLED = "attestation_bypass_enabled";

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
        String storedSetupPayload = preferences.getString(KEY_SETUP_PAYLOAD, "");
        String storedOpenHabBaseUrl = preferences.getString(KEY_OPENHAB_BASE_URL, "");
        String storedOpenHabApiToken = preferences.getString(KEY_OPENHAB_API_TOKEN, "");
        String storedOtbrBaseUrl = preferences.getString(KEY_OTBR_BASE_URL, "");
        boolean storedAttestationBypassEnabled = preferences.getBoolean(KEY_ATTESTATION_BYPASS_ENABLED, false);
        AppConfig config = mapper.fromStoredValues(
                storedThreadDataset,
                storedSetupPayload,
                storedOpenHabBaseUrl,
                storedOpenHabApiToken,
                storedOtbrBaseUrl,
                storedAttestationBypassEnabled);
        if (mapper.isLegacyPlaintextThreadDataset(storedThreadDataset)
                || mapper.isLegacyPlaintextOpenHabApiToken(storedOpenHabApiToken)) {
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
                    .putString(KEY_SETUP_PAYLOAD, storedConfig.setupPayload())
                    .putString(KEY_OPENHAB_BASE_URL, storedConfig.openHabBaseUrl())
                    .putString(KEY_OPENHAB_API_TOKEN, storedConfig.openHabApiToken())
                    .putString(KEY_OTBR_BASE_URL, storedConfig.otbrBaseUrl())
                    .putBoolean(KEY_ATTESTATION_BYPASS_ENABLED, config.attestationBypassEnabled())
                    .apply();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save app config", e);
        }
    }
}
