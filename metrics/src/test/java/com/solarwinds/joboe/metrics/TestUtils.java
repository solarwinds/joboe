package com.solarwinds.joboe.metrics;

import com.solarwinds.joboe.core.settings.SettingsManager;
import com.solarwinds.joboe.core.settings.SimpleSettingsFetcher;
import com.solarwinds.joboe.core.settings.TestSettingsReader;

public abstract class TestUtils {
    public static TestSettingsReader testSettingsReader = initReader();

    private static TestSettingsReader initReader() {
        TestSettingsReader reader = new TestSettingsReader();
        SettingsManager.initializeFetcher(new SimpleSettingsFetcher(reader));
        return reader;
    }
}
