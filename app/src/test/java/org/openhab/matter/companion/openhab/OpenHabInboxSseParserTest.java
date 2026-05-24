package org.openhab.matter.companion.openhab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OpenHabInboxSseParserTest {
    @Test
    public void parsesMatterInboxEventFromSseBlock() {
        OpenHabInboxEvent event = OpenHabInboxSseParser.parse(
                "event: message\n"
                        + "data: {\"topic\":\"openhab/inbox/matter:node:abc/added\",\"payload\":{\"thingUID\":\"matter:node:abc\"}}\n"
                        + "\n");

        assertTrue(event.matterEntryDetected());
        assertEquals("openhab/inbox/matter:node:abc/added", event.topic());
    }

    @Test
    public void parsesMultiLineDataEvent() {
        OpenHabInboxEvent event = OpenHabInboxSseParser.parse(
                "data: {\"topic\":\"openhab/inbox/other/added\",\n"
                        + "data: \"payload\":{\"thingUID\":\"matter:node:abc\"}}\n"
                        + "\n");

        assertEquals("{\"topic\":\"openhab/inbox/other/added\",\n\"payload\":{\"thingUID\":\"matter:node:abc\"}}",
                event.rawData());
        assertTrue(event.matterEntryDetected());
    }

    @Test
    public void ignoresNonMatterInboxEvent() {
        OpenHabInboxEvent event = OpenHabInboxSseParser.parse(
                "data: {\"topic\":\"openhab/items/Switch/state\",\"payload\":\"ON\"}\n\n");

        assertFalse(event.matterEntryDetected());
    }

    @Test
    public void returnsEmptyEventWhenBlockHasNoDataLines() {
        OpenHabInboxEvent event = OpenHabInboxSseParser.parse("event: message\nid: 1\n\n");

        assertEquals("", event.topic());
        assertEquals("", event.rawData());
        assertFalse(event.matterEntryDetected());
    }
}
