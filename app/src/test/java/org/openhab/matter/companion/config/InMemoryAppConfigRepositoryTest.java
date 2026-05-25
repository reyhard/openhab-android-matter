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
        assertEquals("", config.otbrBaseUrl());
    }

    @Test
    public void emptyRepositoryDefaultsAttestationBypassToFalse() {
        AppConfigRepository repository = new InMemoryAppConfigRepository();

        AppConfig config = repository.load();

        assertEquals(false, config.attestationBypassEnabled());
    }

    @Test
    public void saveAndLoadPersistsDatasetOpenHabBaseUrlAndOtbrBaseUrl() {
        AppConfigRepository repository = new InMemoryAppConfigRepository();

        repository.save(new AppConfig("hex:0E080000000000010000", "http://openhab.local:8080", "http://otbr.local"));
        AppConfig config = repository.load();

        assertEquals("hex:0E080000000000010000", config.threadDataset());
        assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
        assertEquals("http://otbr.local", config.otbrBaseUrl());
    }

    @Test
    public void saveAndLoadPersistsAttestationBypass() {
        AppConfigRepository repository = new InMemoryAppConfigRepository();

        repository.save(new AppConfig(
                "hex:0E080000000000010000",
                "http://openhab.local:8080",
                "http://otbr.local",
                false,
                true));
        AppConfig config = repository.load();

        assertEquals(true, config.attestationBypassEnabled());
    }
}
