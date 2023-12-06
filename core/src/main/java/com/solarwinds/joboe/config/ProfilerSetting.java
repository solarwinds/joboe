package com.solarwinds.joboe.config;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ProfilerSetting implements Serializable {
    public static final int DEFAULT_INTERVAL = 20;
    public static final int MIN_INTERVAL = 10;
    public static final int DEFAULT_CIRCUIT_BREAKER_DURATION_THRESHOLD = 100;
    public static final int DEFAULT_CIRCUIT_BREAKER_COUNT_THRESHOLD = 2;
    public static final Set<String> DEFAULT_EXCLUDE_PACKAGES = new HashSet<String>(Arrays.asList("java", "javax", "com.sun", "sun", "sunw"));
    private final boolean isEnabled;
    private final Set<String> excludePackages;
    private final int interval;
    private final int circuitBreakerDurationThreshold;
    private final int circuitBreakerCountThreshold;
    
    public ProfilerSetting(boolean isEnabled, Set<String> excludePackages, int interval, int circuitBreakerDurationThreshold, int circuitBreakerCountThreshold) {
        super();
        this.isEnabled = isEnabled;
        this.excludePackages = excludePackages;
        this.interval = interval;
        this.circuitBreakerDurationThreshold = circuitBreakerDurationThreshold;
        this.circuitBreakerCountThreshold = circuitBreakerCountThreshold;
    }
    
    public ProfilerSetting(boolean isEnabled, int interval) {
        this(isEnabled, DEFAULT_EXCLUDE_PACKAGES, interval, DEFAULT_CIRCUIT_BREAKER_DURATION_THRESHOLD, DEFAULT_CIRCUIT_BREAKER_COUNT_THRESHOLD);
    }
    
    public boolean isEnabled() {
        return isEnabled;
    }
    
    public int getInterval() {
        return interval;
    }
    
    public Set<String> getExcludePackages() {
        return excludePackages;
    }
    
    public int getCircuitBreakerDurationThreshold() {
        return circuitBreakerDurationThreshold;
    }
    
    public int getCircuitBreakerCountThreshold() {
        return circuitBreakerCountThreshold;
    }

    @Override
    public String toString() {
        return "ProfilerSetting [isEnabled=" + isEnabled + ", excludePackages=" + excludePackages + ", interval=" + interval + ", circuitBreakerDurationThreshold="
                + circuitBreakerDurationThreshold + ", circuitBreakerCountThreshold=" + circuitBreakerCountThreshold + "]";
    }
}
