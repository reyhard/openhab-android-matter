package org.openhab.matter.companion.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ThreadDatasetTest {
    @Test
    public void normalizesHexPrefixAndWhitespace() {
        ThreadDataset dataset = ThreadDataset.parse(" hex: 0E08 0000\t0000\n0001 0000 ");
        assertEquals("0E080000000000010000", dataset.hex());
        assertEquals("hex:0E080000000000010000", dataset.chipToolValue());
    }

    @Test
    public void rejectsOddLengthHex() {
        assertThrows(IllegalArgumentException.class, () -> ThreadDataset.parse("ABC"));
    }

    @Test
    public void rejectsNonHexText() {
        assertThrows(IllegalArgumentException.class, () -> ThreadDataset.parse("hex:not-a-dataset"));
    }
}
