package org.openhab.matter.companion.openhab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FakeOpenHabMatterDiscoveryClientTest {
    @Test
    public void startedReturnsSuccessfulScanStatus() throws Exception {
        OpenHabMatterDiscoveryClient client = FakeOpenHabMatterDiscoveryClient.started(120);

        OpenHabMatterDiscoveryScanStatus status = client.startMatterScan("http://openhab.local:8080",
                "34970112332", "oh.test.token");

        assertTrue(status.reachable());
        assertTrue(status.started());
        assertEquals("openHAB Matter scan started", status.message());
        assertEquals(120, status.timeoutSeconds());
        assertEquals("Simulated Matter scan start for http://openhab.local:8080", status.details());
    }

    @Test
    public void failedReturnsFailureStatus() throws Exception {
        OpenHabMatterDiscoveryClient client = FakeOpenHabMatterDiscoveryClient.failed(
                "openHAB Matter scan could not be started", "Simulated failure");

        OpenHabMatterDiscoveryScanStatus status = client.startMatterScan("http://openhab.local:8080",
                "34970112332", "");

        assertFalse(status.reachable());
        assertFalse(status.started());
        assertEquals("openHAB Matter scan could not be started", status.message());
        assertEquals(0, status.timeoutSeconds());
        assertEquals("Simulated failure", status.details());
    }
}
