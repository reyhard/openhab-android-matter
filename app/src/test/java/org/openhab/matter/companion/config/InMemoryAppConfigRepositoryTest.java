package org.openhab.matter.companion.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InMemoryAppConfigRepositoryTest {
    @Test
    public void emptyRepositoryReturnsEmptyConfig() {
        AppConfigRepository repository = new InMemoryAppConfigRepository();

        AppConfig config = repository.load();

        assertEquals("", config.threadDataset());
        assertEquals("", config.openHabBaseUrl());
    }

    @Test
    public void saveAndLoadPersistsDatasetAndOpenHabBaseUrl() {
        AppConfigRepository repository = new InMemoryAppConfigRepository();

        repository.save(new AppConfig("hex:0E080000000000010000", "http://openhab.local:8080"));
        AppConfig config = repository.load();

        assertEquals("hex:0E080000000000010000", config.threadDataset());
        assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
    }
}
