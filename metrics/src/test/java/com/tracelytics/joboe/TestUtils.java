package com.tracelytics.joboe;

import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.joboe.settings.SimpleSettingsFetcher;
import com.tracelytics.joboe.settings.TestSettingsReader;
import junit.framework.TestCase;

public abstract class TestUtils {
    public static TestSettingsReader testSettingsReader = initReader();

    private static TestSettingsReader initReader() {
        TestSettingsReader reader = new TestSettingsReader();
        SettingsManager.initializeFetcher(new SimpleSettingsFetcher(reader));
        return reader;
    }
}
