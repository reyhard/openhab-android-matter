package org.openhab.matter.companion.openhab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FakeOpenHabClientTest {
    @Test
    public void returnsConfiguredOnlineReadiness() throws Exception {
        OpenHabClient client = FakeOpenHabClient.online();

        OpenHabStatus status = client.checkReadiness("http://openhab.local:8080");

        assertTrue(status.online());
        assertTrue(status.restReachable());
        assertTrue(status.matterControllerReady());
        assertEquals("openHAB Matter controller is ready", status.message());
        assertEquals("Simulated readiness check for http://openhab.local:8080", status.details());
    }

    @Test
    public void returnsConfiguredOfflineReadiness() throws Exception {
        OpenHabClient client = FakeOpenHabClient.offline("openHAB REST API is not reachable", "Simulated connection failure");

        OpenHabStatus status = client.checkReadiness("http://openhab.local:8080");

        assertFalse(status.online());
        assertFalse(status.restReachable());
        assertFalse(status.matterControllerReady());
        assertEquals("openHAB REST API is not reachable", status.message());
        assertEquals("Simulated connection failure", status.details());
    }
}
