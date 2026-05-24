package org.openhab.matter.companion.controller;

public interface MatterBootstrapStateRepository {
    MatterBootstrapState load();

    void save(MatterBootstrapState state);

    void clear();
}
