package com.tracelytics.joboe;

/**
 * Matches oboe C library OBOE_SAMPLE_RATE_SOURCE values
 *
 * See oboe_inst_macros.h
 */
public enum SampleRateSource {
    FILE(1),  //locally configured rate, could be from file (agent.sampleRate or url patterns) or JVM args
    DEFAULT(2),
    OBOE(3),
    LAST_OBOE(4),
    DEFAULT_MISCONFIGURED(5),
    OBOE_DEFAULT(6);

    private final int value;
    
    SampleRateSource(int value) {
        this.value = value;    
    }
    
    public int value() {
        return value;
    }

}