package org.openhab.matter.companion.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MatterHandoffCodeParserTest {
    @Test
    public void acceptsMatterQrPayloadForOpenHabScanInput() {
        assertEquals("MT:Y.K9042C00KA0648G00",
                MatterHandoffCodeParser.parseForOpenHabScanInput("  MT:Y.K9042C00KA0648G00  "));
    }

    @Test
    public void acceptsMatterQrPayloadWithOptionalDataForOpenHabScanInput() {
        assertEquals("MT:Y.K9042C00KA064IJ3P0O0",
                MatterHandoffCodeParser.parseForOpenHabScanInput("  MT:Y.K9042C00KA064IJ3P0O0  "));
    }

    @Test
    public void rejectsMatterQrPayloadWithInvalidDecodedPinForOpenHabScanInput() {
        assertThrows(IllegalArgumentException.class,
                () -> MatterHandoffCodeParser.parseForOpenHabScanInput(MatterQrPayloadParserTest.qrPayloadWithPin(11111111)));
    }

    @Test
    public void acceptsHyphenatedManualCodeForOpenHabScanInput() {
        assertEquals("34970112332",
                MatterHandoffCodeParser.parseForOpenHabScanInput("3497-0112-332"));
    }

    @Test
    public void acceptsCompactManualCodeForOpenHabScanInput() {
        assertEquals("34970112332",
                MatterHandoffCodeParser.parseForOpenHabScanInput("34970112332"));
    }

    @Test
    public void acceptsLongManualCodeForOpenHabScanInput() {
        assertEquals("641295075300001000017",
                MatterHandoffCodeParser.parseForOpenHabScanInput("6412-9507-5300-0010-0001-7"));
    }

    @Test
    public void rejectsManualCodeWithWrongCheckDigit() {
        assertThrows(IllegalArgumentException.class,
                () -> MatterHandoffCodeParser.parseForOpenHabScanInput("34970112333"));
    }

    @Test
    public void rejectsManualCodeWithInvalidDecodedPin() {
        assertThrows(IllegalArgumentException.class,
                () -> MatterHandoffCodeParser.parseForOpenHabScanInput("23276800006"));
    }

    @Test
    public void rejectsLongManualCodeWithInvalidVendorAndProductRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> MatterHandoffCodeParser.parseForOpenHabScanInput("641295075399999999991"));
    }

    @Test
    public void rejectsLongManualCodeWithInvalidProductRange() {
        assertThrows(IllegalArgumentException.class,
                () -> MatterHandoffCodeParser.parseForOpenHabScanInput("641295075300001999992"));
    }

    @Test
    public void rejectsThreadOnlyExplicitFieldsForOpenHabScanInput() {
        assertThrows(IllegalArgumentException.class,
                () -> MatterHandoffCodeParser.parseForOpenHabScanInput("pin=20202021;disc=3840"));
    }

    @Test
    public void rejectsUnrelatedTextForOpenHabScanInput() {
        assertThrows(IllegalArgumentException.class,
                () -> MatterHandoffCodeParser.parseForOpenHabScanInput("https://example.test"));
    }
}
