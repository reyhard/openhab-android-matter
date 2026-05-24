package org.openhab.matter.companion.controller;

import org.junit.Test;
import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FakeMatterControllerTest {
    @Test
    public void commissionsThreadDeviceAndReturnsNodeId() throws Exception {
        MatterController controller = new FakeMatterController();
        List<CommissioningStep> steps = new ArrayList<>();
        long nodeId = controller.commissionBleThread(
                ThreadDataset.parse("0E080000000000010000"),
                new MatterSetupPayload("pin=20202021;disc=3840", 20202021L, 3840, "Aqara", "U200", false),
                steps::add);

        assertEquals(1L, nodeId);
        assertEquals("Simulated: Thread network join completed", steps.get(steps.size() - 1).message());
    }

    @Test
    public void fakeProgressDoesNotExposePinOrDataset() throws Exception {
        MatterController controller = new FakeMatterController();
        List<CommissioningStep> steps = new ArrayList<>();
        controller.commissionBleThread(
                ThreadDataset.parse("0E080000000000010000"),
                new MatterSetupPayload("pin=20202021;disc=3840", 20202021L, 3840, "Aqara", "U200", false),
                steps::add);

        for (CommissioningStep step : steps) {
            assertTrue(step.message().startsWith("Simulated:"));
            assertTrue(!step.message().contains("20202021"));
            assertTrue(!step.message().contains("0E080000000000010000"));
        }
    }

    @Test
    public void opensCommissioningWindowWithTemporaryCode() throws Exception {
        MatterController controller = new FakeMatterController();
        String code = controller.openCommissioningWindow(1L, 300, 3840, ignored -> { });
        assertTrue(code.matches("\\d{4}-\\d{4}-\\d{3}"));
    }
}
