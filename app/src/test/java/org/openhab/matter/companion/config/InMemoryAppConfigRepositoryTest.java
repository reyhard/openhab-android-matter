package org.openhab.matter.companion.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InMemoryAppConfigRepositoryTest {
    @Test
    public void emptyRepositoryReturnsEmptyConfig() {
        AppConfigRepository repository = new InMemoryAppConfigRepository();

        AppConfig config = repository.load();

        assertEquals("", config.threadDataset());
        assertEquals("", config.setupPayload());
        assertEquals("", config.openHabBaseUrl());
        assertEquals("", config.openHabApiToken());
        assertEquals("", config.otbrBaseUrl());
    }

    @Test
    public void emptyRepositoryDefaultsAttestationBypassToFalse() {
        AppConfigRepository repository = new InMemoryAppConfigRepository();

        AppConfig config = repository.load();

        assertEquals(false, config.attestationBypassEnabled());
    }

    @Test
    public void saveAndLoadPersistsDatasetOpenHabUrlTokenAndOtbrBaseUrl() {
        AppConfigRepository repository = new InMemoryAppConfigRepository();

        repository.save(new AppConfig(
                "hex:0E080000000000010000",
                "pin=20202021;disc=1740",
                "http://openhab.local:8080",
                "oh.test.token",
                "fd00::1"));
        AppConfig config = repository.load();

        assertEquals("hex:0E080000000000010000", config.threadDataset());
        assertEquals("pin=20202021;disc=1740", config.setupPayload());
        assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
        assertEquals("oh.test.token", config.openHabApiToken());
        assertEquals("fd00::1", config.otbrBaseUrl());
    }

    @Test
    public void saveAndLoadPersistsAttestationBypass() {
        AppConfigRepository repository = new InMemoryAppConfigRepository();

        repository.save(new AppConfig(
                "hex:0E080000000000010000",
                "",
                "http://openhab.local:8080",
                "oh.test.token",
                "http://otbr.local",
                false,
                true));
        AppConfig config = repository.load();

        assertEquals(true, config.attestationBypassEnabled());
    }
}
