package org.openhab.matter.companion.controller;

import android.content.SharedPreferences;

import org.openhab.matter.companion.config.SecretCodec;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SharedPreferencesMatterBootstrapStateRepositoryTest {
    @Test
    public void saveCommitsSynchronously() {
        FakeSharedPreferences preferences = new FakeSharedPreferences();
        SharedPreferencesMatterBootstrapStateRepository repository = repository(preferences);

        repository.save(new MatterBootstrapState(1234L, "opaque-state", false, "Aqara", "U200"));

        assertEquals(1, preferences.commitCount);
        assertEquals(0, preferences.applyCount);
        assertEquals(1234L, preferences.getLong("bootstrap_node_id", -1L));
        assertEquals("enc:v1:encoded(opaque-state)", preferences.getString("controller_state", ""));
        assertEquals("Aqara", preferences.getString("vendor_name", ""));
        assertEquals("U200", preferences.getString("product_name", ""));
    }

    @Test
    public void loadIncludesVendorAndProduct() {
        FakeSharedPreferences preferences = new FakeSharedPreferences();
        SharedPreferencesMatterBootstrapStateRepository repository = repository(preferences);
        repository.save(new MatterBootstrapState(1234L, "opaque-state", false, "Aqara", "U200"));

        MatterBootstrapState state = repository.load();

        assertEquals("Aqara", state.vendorName());
        assertEquals("U200", state.productName());
    }

    @Test
    public void loadIncludesFetchedDeviceDetails() {
        FakeSharedPreferences preferences = new FakeSharedPreferences();
        SharedPreferencesMatterBootstrapStateRepository repository = repository(preferences);
        MatterDeviceDetails details = new MatterDeviceDetails.Builder()
                .softwareVersionString("1.8.7")
                .hardwareVersionString("P2.0")
                .partNumber("PN-1")
                .batteryPercentRemaining(98)
                .batteryQuantity(2)
                .batteryDesignation("AAA")
                .threadNetworkName("OpenThread")
                .threadChannel(25)
                .ipv6Address("fd88:9326:57d6:1:a3b:edad:e81e:1f1e")
                .otaUpdatePossible(false)
                .build();
        repository.save(new MatterBootstrapState(1234L, "opaque-state", false, "Aqara", "U200", details));

        MatterDeviceDetails loaded = repository.load().deviceDetails();

        assertEquals("1.8.7", loaded.softwareVersionString());
        assertEquals("P2.0", loaded.hardwareVersionString());
        assertEquals("PN-1", loaded.partNumber());
        assertEquals(Integer.valueOf(98), loaded.batteryPercentRemaining());
        assertEquals(Integer.valueOf(2), loaded.batteryQuantity());
        assertEquals("AAA", loaded.batteryDesignation());
        assertEquals("OpenThread", loaded.threadNetworkName());
        assertEquals(Integer.valueOf(25), loaded.threadChannel());
        assertEquals("fd88:9326:57d6:1:a3b:edad:e81e:1f1e", loaded.ipv6Address());
        assertEquals(Boolean.FALSE, loaded.otaUpdatePossible());
    }

    @Test
    public void clearCommitsSynchronously() {
        FakeSharedPreferences preferences = new FakeSharedPreferences();
        SharedPreferencesMatterBootstrapStateRepository repository = repository(preferences);
        repository.save(new MatterBootstrapState(1234L, "opaque-state", false));

        repository.clear();

        assertEquals(2, preferences.commitCount);
        assertEquals(0, preferences.applyCount);
        assertEquals(false, preferences.contains("bootstrap_node_id"));
        assertEquals(false, preferences.contains("controller_state"));
        assertEquals(false, preferences.contains("software_version_string"));
        assertEquals(false, preferences.contains("battery_percent_remaining"));
        assertEquals(false, preferences.contains("thread_channel"));
        assertEquals(false, preferences.contains("ota_update_possible"));
    }

    @Test
    public void failedSaveCommitThrows() {
        FakeSharedPreferences preferences = new FakeSharedPreferences();
        preferences.commitResult = false;
        SharedPreferencesMatterBootstrapStateRepository repository = repository(preferences);

        assertThrows(IllegalStateException.class,
                () -> repository.save(new MatterBootstrapState(1234L, "opaque-state", false)));
    }

    @Test
    public void malformedPreferenceTypesLoadAsUnreadableState() {
        FakeSharedPreferences preferences = new FakeSharedPreferences();
        preferences.values.put("bootstrap_node_id", "not-a-long");
        preferences.values.put("controller_state", 123);
        SharedPreferencesMatterBootstrapStateRepository repository = repository(preferences);

        MatterBootstrapState state = repository.load();

        assertEquals(-1L, state.bootstrapNodeId());
        assertEquals("", state.controllerState());
        assertEquals(true, state.stateUnreadable());
    }

    private static SharedPreferencesMatterBootstrapStateRepository repository(FakeSharedPreferences preferences) {
        return new SharedPreferencesMatterBootstrapStateRepository(
                preferences,
                new SecureMatterBootstrapStateMapper(new FixedSecretCodec()));
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
        private int applyCount;
        private int commitCount;
        private boolean commitResult = true;

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
                commitCount++;
                if (!commitResult) {
                    return false;
                }
                writePending();
                return true;
            }

            @Override
            public void apply() {
                applyCount++;
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
