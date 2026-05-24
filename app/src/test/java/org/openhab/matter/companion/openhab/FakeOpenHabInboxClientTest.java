package org.openhab.matter.companion.openhab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FakeOpenHabInboxClientTest {
    @Test
    public void returnsConfiguredMatterDeviceDetectedStatus() throws Exception {
        OpenHabInboxClient client = FakeOpenHabInboxClient.matterDeviceDetected();

        OpenHabInboxStatus status = client.checkInbox("http://openhab.local:8080");

        assertTrue(status.reachable());
        assertTrue(status.matterEntryDetected());
        assertEquals("Matter Inbox entry detected", status.message());
        assertEquals("Simulated Inbox check for http://openhab.local:8080", status.details());
    }

    @Test
    public void returnsConfiguredNoMatterDeviceStatus() throws Exception {
        OpenHabInboxClient client = FakeOpenHabInboxClient.noMatterDevice();

        OpenHabInboxStatus status = client.checkInbox("http://openhab.local:8080");

        assertTrue(status.reachable());
        assertFalse(status.matterEntryDetected());
        assertEquals("No Matter Inbox entry detected", status.message());
        assertEquals("Simulated Inbox check for http://openhab.local:8080", status.details());
    }
}
