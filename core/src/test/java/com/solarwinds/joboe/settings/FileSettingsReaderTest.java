package com.solarwinds.joboe.settings;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileSettingsReaderTest {
    private FileSettingsReader tested;
    @Test
    void throwExceptionWhenInLambdaAndFileDoesNotExist() {
        tested = new FileSettingsReader("doesn't-exist");
        assertThrows(OboeSettingsException.class, () -> tested.getSettings());
    }

    @Test
    void returnSettingsWhenInLambdaAndFileDoesExist() throws OboeSettingsException {
        tested = new FileSettingsReader(new File("src/test/resources/solarwinds-apm-settings-raw").getPath());
        Map<String, Settings> settingsMap = tested.getSettings();

        assertNotNull(settingsMap);
        boolean anyMatch = settingsMap.values().stream().anyMatch(settings -> settings.getTtl() == 120);
        assertTrue(anyMatch);
    }

}