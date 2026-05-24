package org.openhab.matter.companion.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class MatterSetupPayloadParserTest {
    @Test
    public void parsesExplicitFieldsForThreadCommissioning() {
        MatterSetupPayload payload = MatterSetupPayloadParser.parse("pin=20202021;disc=3840;vendor=Aqara;product=U200");
        assertEquals(20202021L, payload.pin());
        assertEquals(3840, payload.discriminator());
        assertEquals("Aqara", payload.vendorName());
        assertEquals("U200", payload.productName());
    }

    @Test
    public void parsesQrPayloadLocally() {
        MatterSetupPayload payload = MatterSetupPayloadParser.parse("MT:Y.K9042C00KA0648G00");
        assertEquals("MT:Y.K9042C00KA0648G00", payload.rawPayload());
        assertEquals(20202021L, payload.pin());
        assertEquals(3840, payload.discriminator());
        assertFalse(payload.requiresChipParser());
    }

    @Test
    public void rejectsManualCodeWithoutLongDiscriminatorForThread() {
        assertThrows(IllegalArgumentException.class, () -> MatterSetupPayloadParser.parse("34970112332"));
    }

    @Test
    public void rejectsExplicitPinThatIsNotEightDigits() {
        assertThrows(IllegalArgumentException.class, () -> MatterSetupPayloadParser.parse("pin=1234;disc=3840"));
        assertThrows(IllegalArgumentException.class, () -> MatterSetupPayloadParser.parse("pin=123456789;disc=3840"));
    }
}
