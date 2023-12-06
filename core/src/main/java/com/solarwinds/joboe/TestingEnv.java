package com.solarwinds.joboe;

import com.solarwinds.joboe.settings.TestSettingsReader;

public class TestingEnv {
    private TestReporter tracingReporter;
    private TestSettingsReader settingsReader;
    private TestReporter profilingReporter;
    
    public TestingEnv(TestReporter tracingReporter, TestReporter profilingReporter, TestSettingsReader settingsReader) {
        super();
        this.tracingReporter = tracingReporter;
        this.profilingReporter = profilingReporter;
        this.settingsReader = settingsReader;
    }
    
    public TestReporter getTracingReporter() {
        return tracingReporter;
    }
    
    public TestReporter getProfilingReporter() {
        return profilingReporter;
    }
    
    public TestSettingsReader getSettingsReader() {
        return settingsReader;
    }
}
