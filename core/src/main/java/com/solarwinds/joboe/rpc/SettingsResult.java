package com.solarwinds.joboe.rpc;

import java.util.List;

import com.solarwinds.joboe.settings.Settings;
import lombok.Getter;

@Getter
public class SettingsResult extends Result {
    private final List<Settings> settings;
    
    public SettingsResult(ResultCode resultCode, String arg, String warning, List<Settings> settings) {
        super(resultCode, arg, warning);
        this.settings = settings;
    }

}
