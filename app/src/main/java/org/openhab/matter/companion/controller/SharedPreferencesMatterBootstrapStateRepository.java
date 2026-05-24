package org.openhab.matter.companion.controller;

import android.content.Context;
import android.content.SharedPreferences;

import org.openhab.matter.companion.config.AndroidKeystoreSecretCodec;

public final class SharedPreferencesMatterBootstrapStateRepository implements MatterBootstrapStateRepository {
    private static final String PREFERENCE_FILE = "openhab_matter_bootstrap_state";
    private static final String KEY_BOOTSTRAP_NODE_ID = "bootstrap_node_id";
    private static final String KEY_CONTROLLER_STATE = "controller_state";

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
        long bootstrapNodeId = preferences.getLong(KEY_BOOTSTRAP_NODE_ID, -1L);
        String controllerState = preferences.getString(KEY_CONTROLLER_STATE, "");
        return mapper.fromStoredValues(bootstrapNodeId, controllerState);
    }

    @Override
    public void save(MatterBootstrapState state) {
        try {
            SecureMatterBootstrapStateMapper.StoredBootstrapState storedState = mapper.toStoredValues(state);
            preferences.edit()
                    .putLong(KEY_BOOTSTRAP_NODE_ID, storedState.bootstrapNodeId())
                    .putString(KEY_CONTROLLER_STATE, storedState.controllerState())
                    .apply();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save Matter bootstrap state", e);
        }
    }

    @Override
    public void clear() {
        preferences.edit()
                .remove(KEY_BOOTSTRAP_NODE_ID)
                .remove(KEY_CONTROLLER_STATE)
                .apply();
    }
}
