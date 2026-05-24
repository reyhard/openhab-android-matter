package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MatterBootstrapStateResolverTest {
    @Test
    public void persistedNodeIdWinsOverStaleSavedInstanceNodeId() {
        long nodeId = MatterBootstrapStateResolver.resolveNodeId(
                -1L,
                new MatterBootstrapState(1234L, "", false));

        assertEquals(1234L, nodeId);
    }

    @Test
    public void savedInstanceNodeIdIsUsedWhenPersistedStateIsEmpty() {
        long nodeId = MatterBootstrapStateResolver.resolveNodeId(
                42L,
                MatterBootstrapState.empty());

        assertEquals(42L, nodeId);
    }

    @Test
    public void savedInstanceNodeIdIsUsedWhenPersistedStateIsUnreadable() {
        long nodeId = MatterBootstrapStateResolver.resolveNodeId(
                42L,
                new MatterBootstrapState(-1L, "", true));

        assertEquals(42L, nodeId);
    }
}
