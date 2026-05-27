package org.openhab.matter.companion.config;

import android.content.SharedPreferences;

import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class SharedPreferencesAppConfigRepositoryTest {
    @Test
    public void loadDefaultsAttestationBypassToFalseWhenPreferenceMissing() {
        SharedPreferencesAppConfigRepository repository = repository(new FakeSharedPreferences());

        AppConfig config = repository.load();

        assertEquals(false, config.attestationBypassEnabled());
    }

    @Test
    public void saveAndLoadPersistsAttestationBypassBoolean() {
        FakeSharedPreferences preferences = new FakeSharedPreferences();
        SharedPreferencesAppConfigRepository repository = repository(preferences);

        repository.save(new AppConfig(
                "hex:0E080000000000010000",
                "pin=20202021;disc=1740",
                "http://openhab.local:8080",
                "oh.test.token",
                "http://otbr.local",
                false,
                true));

        assertEquals(true, preferences.getBoolean("attestation_bypass_enabled", false));
        assertEquals("enc:v1:encoded(pin=20202021;disc=1740)", preferences.getString("setup_payload", ""));
        assertEquals("enc:v1:encoded(oh.test.token)", preferences.getString("openhab_api_token", ""));
        assertEquals(true, repository.load().attestationBypassEnabled());
        assertEquals("pin=20202021;disc=1740", repository.load().setupPayload());
        assertEquals("oh.test.token", repository.load().openHabApiToken());
    }

    @Test
    public void loadMigratesLegacyPlaintextOpenHabTokenToEncryptedStorage() {
        FakeSharedPreferences preferences = new FakeSharedPreferences();
        preferences.edit()
                .putString("thread_dataset", "enc:v1:encoded(hex:001122)")
                .putString("openhab_base_url", "http://openhab.local:8080")
                .putString("openhab_api_token", "oh.legacy.token")
                .putString("otbr_base_url", "fd00::1")
                .apply();
        SharedPreferencesAppConfigRepository repository = repository(preferences);

        AppConfig config = repository.load();

        assertEquals("oh.legacy.token", config.openHabApiToken());
        assertEquals("enc:v1:encoded(oh.legacy.token)", preferences.getString("openhab_api_token", ""));
    }

    private static SharedPreferencesAppConfigRepository repository(FakeSharedPreferences preferences) {
        return new SharedPreferencesAppConfigRepository(
                preferences,
                new SecureAppConfigMapper(new FixedSecretCodec()));
    }

    private static final class FixedSecretCodec implements SecretCodec {
        @Override
        public String encode(String plaintext) {
            if (plaintext == null || plaintext.isEmpty()) {
                return "";
            }
            return "enc:v1:encoded(" + plaintext + ")";
        }

        @Override
        public String decode(String encoded) throws GeneralSecurityException {
            if (encoded == null || encoded.isEmpty()) {
                return "";
            }
            if (!encoded.startsWith("enc:v1:encoded(") || !encoded.endsWith(")")) {
                throw new GeneralSecurityException("broken");
            }
            return encoded.substring("enc:v1:encoded(".length(), encoded.length() - 1);
        }
    }

    private static final class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(values);
        }

        @Override
        public String getString(String key, String defValue) {
            Object value = values.get(key);
            if (value == null) {
                return defValue;
            }
            if (!(value instanceof String)) {
                throw new ClassCastException(key);
            }
            return (String) value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object value = values.get(key);
            if (value == null) {
                return defValues;
            }
            if (!(value instanceof Set)) {
                throw new ClassCastException(key);
            }
            return new HashSet<>((Set<String>) value);
        }

        @Override
        public int getInt(String key, int defValue) {
            Object value = values.get(key);
            if (value == null) {
                return defValue;
            }
            if (!(value instanceof Integer)) {
                throw new ClassCastException(key);
            }
            return (Integer) value;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object value = values.get(key);
            if (value == null) {
                return defValue;
            }
            if (!(value instanceof Long)) {
                throw new ClassCastException(key);
            }
            return (Long) value;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object value = values.get(key);
            if (value == null) {
                return defValue;
            }
            if (!(value instanceof Float)) {
                throw new ClassCastException(key);
            }
            return (Float) value;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key);
            if (value == null) {
                return defValue;
            }
            if (!(value instanceof Boolean)) {
                throw new ClassCastException(key);
            }
            return (Boolean) value;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new FakeEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        private final class FakeEditor implements Editor {
            private final Map<String, Object> pending = new HashMap<>();
            private final Set<String> removals = new HashSet<>();
            private boolean clear;

            @Override
            public Editor putString(String key, String value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, Set<String> values) {
                pending.put(key, values == null ? null : new HashSet<>(values));
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                removals.add(key);
                return this;
            }

            @Override
            public Editor clear() {
                clear = true;
                return this;
            }

            @Override
            public boolean commit() {
                writePending();
                return true;
            }

            @Override
            public void apply() {
                writePending();
            }

            private void writePending() {
                if (clear) {
                    values.clear();
                }
                for (String key : removals) {
                    values.remove(key);
                }
                values.putAll(pending);
            }
        }
    }
}
