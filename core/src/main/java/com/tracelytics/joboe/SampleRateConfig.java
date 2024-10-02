package com.tracelytics.joboe;

/**
 * @deprecated Use {@link TraceConfig} instead. Kept for backward compatibility with TraceView API usage
 * @author pluk
 *
 */
public class SampleRateConfig {
    private final int sampleRate;
    private final SampleRateSource sampleRateSource;
    
    SampleRateConfig(TraceConfig traceConfig) {
        super();
        this.sampleRate = traceConfig.isSampleRateConfigured() ? traceConfig.getSampleRate() : 0;
        this.sampleRateSource = traceConfig.getSampleRateSource();
    }

    public int getSampleRate() {
        return sampleRate;
    }
    
    public int getSampleRateSourceValue() {
        return sampleRateSource.value();
    }
}
