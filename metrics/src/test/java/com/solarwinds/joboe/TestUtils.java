package com.solarwinds.joboe;

import com.solarwinds.joboe.settings.SettingsManager;
import com.solarwinds.joboe.settings.SimpleSettingsFetcher;
import com.solarwinds.joboe.settings.TestSettingsReader;

public abstract class TestUtils {
    public static TestSettingsReader testSettingsReader = initReader();

    private static TestSettingsReader initReader() {
        TestSettingsReader reader = new TestSettingsReader();
        SettingsManager.initializeFetcher(new SimpleSettingsFetcher(reader));
        return reader;
    }
}
