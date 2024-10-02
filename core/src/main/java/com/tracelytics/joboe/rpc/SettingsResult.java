package com.tracelytics.joboe.rpc;

import java.util.List;

import com.tracelytics.joboe.settings.Settings;

public class SettingsResult extends Result {
    private List<Settings> settings;
    
    public SettingsResult(ResultCode resultCode, String arg, String warning, List<Settings> settings) {
        super(resultCode, arg, warning);
        this.settings = settings;
    }
    
    public List<Settings> getSettings() {
        return settings;
    }

}
