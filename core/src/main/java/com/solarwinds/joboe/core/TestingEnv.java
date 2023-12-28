package com.solarwinds.joboe.core;

import com.solarwinds.joboe.core.settings.TestSettingsReader;
import lombok.Getter;

@Getter
public class TestingEnv {
    private final TestReporter tracingReporter;
    private final TestSettingsReader settingsReader;
    private final TestReporter profilingReporter;
    
    public TestingEnv(TestReporter tracingReporter, TestReporter profilingReporter, TestSettingsReader settingsReader) {
        super();
        this.tracingReporter = tracingReporter;
        this.profilingReporter = profilingReporter;
        this.settingsReader = settingsReader;
    }

}
