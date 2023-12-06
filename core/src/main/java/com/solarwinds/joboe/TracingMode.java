package com.solarwinds.joboe;

import static com.solarwinds.joboe.settings.Settings.OBOE_SETTINGS_FLAG_SAMPLE_START;
import static com.solarwinds.joboe.settings.Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;
import static com.solarwinds.joboe.settings.Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;

public enum TracingMode {
    ALWAYS ("always"), //deprecated
    ENABLED ("enabled"),
    NEVER ("never"), //deprecated
    DISABLED ("disabled");
    
    private String stringValue;
    
    private TracingMode(String stringValue) {
        this.stringValue = stringValue;
    }
    
    public static TracingMode fromString(String stringValue) {
        for (TracingMode mode : values()) {
            if (mode.stringValue.equals(stringValue)) {
                return mode;
            }
        }
        
        return null;
    }
    
    public String getStringValue() {
        return stringValue;
    }
    
    // convert agent tracing mode to settings flags
    // XXX: Using THROUGH_ALWAYS to maintain previous behaviour when setting sample rate from command line/config
    public short toFlags() {
        switch(this) {
            case ALWAYS:
            case ENABLED:
                return OBOE_SETTINGS_FLAG_SAMPLE_START | OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS | OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;
                
            case NEVER:
            case DISABLED:
            default:
                return 0x00;
        }

    }
}