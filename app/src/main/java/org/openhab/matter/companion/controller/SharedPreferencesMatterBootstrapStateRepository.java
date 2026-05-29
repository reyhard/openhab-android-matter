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
            return mapper.fromStoredValues(bootstrapNodeId, controllerState, vendorName, productName);
        } catch (ClassCastException e) {
            return new MatterBootstrapState(-1L, "", true);
        }
    }

    @Override
    public void save(MatterBootstrapState state) {
        try {
            SecureMatterBootstrapStateMapper.StoredBootstrapState storedState = mapper.toStoredValues(state);
            boolean saved = preferences.edit()
                    .putLong(KEY_BOOTSTRAP_NODE_ID, storedState.bootstrapNodeId())
                    .putString(KEY_CONTROLLER_STATE, storedState.controllerState())
                    .putString(KEY_VENDOR_NAME, storedState.vendorName())
                    .putString(KEY_PRODUCT_NAME, storedState.productName())
                    .commit();
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
                .commit();
        if (!cleared) {
            throw new IllegalStateException("Unable to clear Matter bootstrap state");
        }
    }
}
