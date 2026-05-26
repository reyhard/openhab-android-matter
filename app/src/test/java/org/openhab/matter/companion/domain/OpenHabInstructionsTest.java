package org.openhab.matter.companion.domain;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class OpenHabInstructionsTest {
    @Test
    public void troubleshootingGuideCoversThreadOpenHabAndControllerArtifacts() {
        String guide = OpenHabInstructions.troubleshootingGuide();

        assertTrue(guide.contains("IPv6"));
        assertTrue(guide.contains("mDNS/Avahi"));
        assertTrue(guide.contains("OTBR"));
        assertTrue(guide.contains("Matter binding controller"));
        assertTrue(guide.contains("REST API token"));
        assertTrue(guide.contains("connectedhomeip"));
        assertTrue(guide.contains("ChipDeviceController"));
    }
}
