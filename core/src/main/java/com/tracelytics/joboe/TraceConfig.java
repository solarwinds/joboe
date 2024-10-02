package com.tracelytics.joboe;

import static com.tracelytics.joboe.settings.Settings.OBOE_SETTINGS_FLAG_SAMPLE_START;
import static com.tracelytics.joboe.settings.Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;

import com.tracelytics.joboe.settings.Settings;

import java.util.Collections;
import java.util.Map;

/**
 * Sample Rate Configuration
 *
 * @see TraceDecisionUtil
 */
public class TraceConfig {
    private Integer sampleRate;
    private SampleRateSource sampleRateSource;
    private Short flags;
    private Map<TokenBucketType, Double> bucketCapacities;
    private Map<TokenBucketType, Double> bucketRates;
    
    public TraceConfig(Integer sampleRate, SampleRateSource sampleRateSource, Short flags) {
        this(sampleRate, sampleRateSource, flags, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
    }

    public TraceConfig(Integer sampleRate, SampleRateSource sampleRateSource, Short flags, Map<TokenBucketType, Double> bucketCapacities, Map<TokenBucketType, Double> bucketRates) {
        this.sampleRate = sampleRate;
        this.sampleRateSource = sampleRateSource;
        this.flags = flags;
        this.bucketCapacities = bucketCapacities;
        this.bucketRates = bucketRates;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public SampleRateSource getSampleRateSource() {
        return sampleRateSource;
    }
    
    public int getSampleRateSourceValue() {
        return sampleRateSource.value();
    }
    
    public double getBucketCapacity(TokenBucketType bucketType) {
        return bucketCapacities.containsKey(bucketType) ? bucketCapacities.get(bucketType) : 0;
    }
    
    public double getBucketRate(TokenBucketType bucketType) {
        return bucketRates.containsKey(bucketType) ? bucketRates.get(bucketType) : 0;
    }

    public Map<TokenBucketType, Double> getBucketCapacities() {
        return bucketCapacities;
    }

    public Map<TokenBucketType, Double> getBucketRates() {
        return bucketRates;
    }

    public boolean hasOverrideFlag() {
        return flags != null && (flags & Settings.OBOE_SETTINGS_FLAG_OVERRIDE) == Settings.OBOE_SETTINGS_FLAG_OVERRIDE;
    }

    public boolean hasSampleStartFlag() {
        return flags != null && (flags & Settings.OBOE_SETTINGS_FLAG_SAMPLE_START) == Settings.OBOE_SETTINGS_FLAG_SAMPLE_START;
    }

    public boolean hasSampleThroughFlag() {
        return flags != null && (flags & Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH) == Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH;
    }

    public boolean hasSampleThroughAlwaysFlag() {
        return flags != null && (flags & Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS) == Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;
    }
    
    public boolean isMetricsEnabled() {
        return flags != null && (flags & (OBOE_SETTINGS_FLAG_SAMPLE_START | OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS)) != 0; //for now if those 2 flags are not on, we assume it's metrics disabled
    }
    
    public boolean hasSampleTriggerTraceFlag() {
        return (flags & Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED) == Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;
    }
    
    short getFlags() {
        return flags;
    }
    
    public boolean isFlagsConfigured() {
        return flags != null;
    }
    
    public boolean isSampleRateConfigured() {
        return sampleRate != null;
    }

    @Override
    public String toString() {
        return "SampleRateConfig [sampleRate=" + sampleRate + ", sampleRateSource=" + sampleRateSource + ", flags=" + flags + ", bucketCapacities=" + bucketCapacities + ", bucketRates="
                + bucketRates + "]";
    }
}
