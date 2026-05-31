package org.openhab.matter.companion.controller;

import android.content.Context;
import android.content.SharedPreferences;

import org.openhab.matter.companion.config.AndroidKeystoreSecretCodec;

public final class SharedPreferencesMatterBootstrapStateRepository implements MatterBootstrapStateRepository {
    private static final String PREFERENCE_FILE = "openhab_matter_bootstrap_state";
    private static final String KEY_BOOTSTRAP_NODE_ID = "bootstrap_node_id";
    private static final String KEY_CONTROLLER_STATE = "controller_state";
    private static final String KEY_VENDOR_NAME = "vendor_name";
    private static final String KEY_PRODUCT_NAME = "product_name";
    private static final String KEY_SOFTWARE_VERSION_STRING = "software_version_string";
    private static final String KEY_HARDWARE_VERSION_STRING = "hardware_version_string";
    private static final String KEY_PART_NUMBER = "part_number";
    private static final String KEY_BATTERY_PERCENT_REMAINING = "battery_percent_remaining";
    private static final String KEY_BATTERY_QUANTITY = "battery_quantity";
    private static final String KEY_BATTERY_DESIGNATION = "battery_designation";
    private static final String KEY_THREAD_NETWORK_NAME = "thread_network_name";
    private static final String KEY_THREAD_CHANNEL = "thread_channel";
    private static final String KEY_IPV6_ADDRESS = "ipv6_address";
    private static final String KEY_OTA_UPDATE_POSSIBLE = "ota_update_possible";

    private final SharedPreferences preferences;
    private final SecureMatterBootstrapStateMapper mapper;

    public SharedPreferencesMatterBootstrapStateRepository(Context context) {
        this(context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE),
                new SecureMatterBootstrapStateMapper(new AndroidKeystoreSecretCodec()));
    }

    SharedPreferencesMatterBootstrapStateRepository(SharedPreferences preferences,
            SecureMatterBootstrapStateMapper mapper) {
        this.preferences = preferences;
        this.mapper = mapper;
    }

    @Override
    public MatterBootstrapState load() {
        try {
            long bootstrapNodeId = preferences.getLong(KEY_BOOTSTRAP_NODE_ID, -1L);
            String controllerState = preferences.getString(KEY_CONTROLLER_STATE, "");
            String vendorName = preferences.getString(KEY_VENDOR_NAME, "");
            String productName = preferences.getString(KEY_PRODUCT_NAME, "");
            MatterDeviceDetails details = loadDeviceDetails();
            return mapper.fromStoredValues(bootstrapNodeId, controllerState, vendorName, productName, details);
        } catch (ClassCastException e) {
            return new MatterBootstrapState(-1L, "", true);
        }
    }

    @Override
    public void save(MatterBootstrapState state) {
        try {
            SecureMatterBootstrapStateMapper.StoredBootstrapState storedState = mapper.toStoredValues(state);
            SharedPreferences.Editor editor = preferences.edit()
                    .putLong(KEY_BOOTSTRAP_NODE_ID, storedState.bootstrapNodeId())
                    .putString(KEY_CONTROLLER_STATE, storedState.controllerState())
                    .putString(KEY_VENDOR_NAME, storedState.vendorName())
                    .putString(KEY_PRODUCT_NAME, storedState.productName())
                    .putString(KEY_SOFTWARE_VERSION_STRING, storedState.deviceDetails().softwareVersionString())
                    .putString(KEY_HARDWARE_VERSION_STRING, storedState.deviceDetails().hardwareVersionString())
                    .putString(KEY_PART_NUMBER, storedState.deviceDetails().partNumber())
                    .putString(KEY_BATTERY_DESIGNATION, storedState.deviceDetails().batteryDesignation())
                    .putString(KEY_THREAD_NETWORK_NAME, storedState.deviceDetails().threadNetworkName())
                    .putString(KEY_IPV6_ADDRESS, storedState.deviceDetails().ipv6Address());
            putOptionalInt(editor, KEY_BATTERY_PERCENT_REMAINING,
                    storedState.deviceDetails().batteryPercentRemaining());
            putOptionalInt(editor, KEY_BATTERY_QUANTITY, storedState.deviceDetails().batteryQuantity());
            putOptionalInt(editor, KEY_THREAD_CHANNEL, storedState.deviceDetails().threadChannel());
            putOptionalBoolean(editor, KEY_OTA_UPDATE_POSSIBLE,
                    storedState.deviceDetails().otaUpdatePossible());
            boolean saved = editor.commit();
            if (!saved) {
                throw new IllegalStateException("Unable to commit Matter bootstrap state");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save Matter bootstrap state", e);
        }
    }

    @Override
    public void clear() {
        boolean cleared = preferences.edit()
                .remove(KEY_BOOTSTRAP_NODE_ID)
                .remove(KEY_CONTROLLER_STATE)
                .remove(KEY_VENDOR_NAME)
                .remove(KEY_PRODUCT_NAME)
                .remove(KEY_SOFTWARE_VERSION_STRING)
                .remove(KEY_HARDWARE_VERSION_STRING)
                .remove(KEY_PART_NUMBER)
                .remove(KEY_BATTERY_PERCENT_REMAINING)
                .remove(KEY_BATTERY_QUANTITY)
                .remove(KEY_BATTERY_DESIGNATION)
                .remove(KEY_THREAD_NETWORK_NAME)
                .remove(KEY_THREAD_CHANNEL)
                .remove(KEY_IPV6_ADDRESS)
                .remove(KEY_OTA_UPDATE_POSSIBLE)
                .commit();
        if (!cleared) {
            throw new IllegalStateException("Unable to clear Matter bootstrap state");
        }
    }

    private MatterDeviceDetails loadDeviceDetails() {
        return new MatterDeviceDetails.Builder()
                .softwareVersionString(preferences.getString(KEY_SOFTWARE_VERSION_STRING, ""))
                .hardwareVersionString(preferences.getString(KEY_HARDWARE_VERSION_STRING, ""))
                .partNumber(preferences.getString(KEY_PART_NUMBER, ""))
                .batteryPercentRemaining(getOptionalInt(KEY_BATTERY_PERCENT_REMAINING))
                .batteryQuantity(getOptionalInt(KEY_BATTERY_QUANTITY))
                .batteryDesignation(preferences.getString(KEY_BATTERY_DESIGNATION, ""))
                .threadNetworkName(preferences.getString(KEY_THREAD_NETWORK_NAME, ""))
                .threadChannel(getOptionalInt(KEY_THREAD_CHANNEL))
                .ipv6Address(preferences.getString(KEY_IPV6_ADDRESS, ""))
                .otaUpdatePossible(getOptionalBoolean(KEY_OTA_UPDATE_POSSIBLE))
                .build();
    }

    private Integer getOptionalInt(String key) {
        return preferences.contains(key) ? preferences.getInt(key, 0) : null;
    }

    private Boolean getOptionalBoolean(String key) {
        return preferences.contains(key) ? preferences.getBoolean(key, false) : null;
    }

    private static void putOptionalInt(SharedPreferences.Editor editor, String key, Integer value) {
        if (value == null) {
            editor.remove(key);
        } else {
            editor.putInt(key, value);
        }
    }

    private static void putOptionalBoolean(SharedPreferences.Editor editor, String key, Boolean value) {
        if (value == null) {
            editor.remove(key);
        } else {
            editor.putBoolean(key, value);
        }
    }
}
