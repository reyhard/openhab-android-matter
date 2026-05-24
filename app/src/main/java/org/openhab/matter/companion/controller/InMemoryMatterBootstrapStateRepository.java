package org.openhab.matter.companion.controller;

public final class InMemoryMatterBootstrapStateRepository implements MatterBootstrapStateRepository {
    private MatterBootstrapState state = MatterBootstrapState.empty();

    @Override
    public MatterBootstrapState load() {
        return state;
    }

    @Override
    public void save(MatterBootstrapState state) {
        this.state = state == null ? MatterBootstrapState.empty() : state;
    }

    @Override
    public void clear() {
        state = MatterBootstrapState.empty();
    }
}
