package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InMemoryMatterBootstrapStateRepositoryTest {
    @Test
    public void loadsEmptyStateByDefault() {
        MatterBootstrapStateRepository repository = new InMemoryMatterBootstrapStateRepository();

        MatterBootstrapState state = repository.load();

        assertEquals(-1L, state.bootstrapNodeId());
        assertEquals("", state.controllerState());
        assertEquals(false, state.stateUnreadable());
    }

    @Test
    public void savesAndLoadsBootstrapState() {
        MatterBootstrapStateRepository repository = new InMemoryMatterBootstrapStateRepository();

        repository.save(new MatterBootstrapState(42L, "opaque-controller-state", false));

        MatterBootstrapState state = repository.load();
        assertEquals(42L, state.bootstrapNodeId());
        assertEquals("opaque-controller-state", state.controllerState());
        assertEquals(false, state.stateUnreadable());
    }

    @Test
    public void clearRestoresEmptyState() {
        MatterBootstrapStateRepository repository = new InMemoryMatterBootstrapStateRepository();
        repository.save(new MatterBootstrapState(42L, "opaque-controller-state", false));

        repository.clear();

        MatterBootstrapState state = repository.load();
        assertEquals(-1L, state.bootstrapNodeId());
        assertEquals("", state.controllerState());
        assertEquals(false, state.stateUnreadable());
    }
}
