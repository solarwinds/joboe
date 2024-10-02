package com.tracelytics.agent.config;

import com.tracelytics.ext.json.JSONArray;
import com.tracelytics.ext.json.JSONException;
import com.tracelytics.ext.json.JSONObject;
import com.tracelytics.joboe.config.ConfigParser;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.config.ProfilerSetting;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class ProfilerSettingParser implements ConfigParser<String, ProfilerSetting> {
    private static final Logger logger = LoggerFactory.getLogger();
    public static final String ENABLED_KEY = "enabled";
    public static final String EXCLUDE_PACKAGES_KEY = "excludePackages";
    public static final String INTERVAL_KEY = "interval";
    public static final String CIRCUIT_BREAKER_DURATION_THRESHOLD = "circuitBreakerDurationThreshold";
    public static final String CIRCUIT_BREAKER_COUNT_THRESHOLD = "circuitBreakerCountThreshold";
    
    public static final ProfilerSettingParser INSTANCE = new ProfilerSettingParser();
    
    private ProfilerSettingParser() {
    }
    

    public ProfilerSetting convert(String javaValue) throws InvalidConfigException {
        String stringValue = (String) javaValue;
        
        try {
            JSONObject jsonObject = new JSONObject(stringValue);
            
            boolean isEnabled  = jsonObject.getBoolean(ENABLED_KEY);
            
            int interval = jsonObject.has(INTERVAL_KEY) ? jsonObject.getInt(INTERVAL_KEY) : ProfilerSetting.DEFAULT_INTERVAL;
            
            Set<String> excludePackages;
            JSONArray excludePackagesArray = jsonObject.optJSONArray(EXCLUDE_PACKAGES_KEY);
            if (excludePackagesArray != null) {
                excludePackages = new HashSet<String>();
                for (int i = 0; i < excludePackagesArray.length(); i ++) {
                    excludePackages.add(excludePackagesArray.getString(i));
                }
            } else {
                excludePackages = ProfilerSetting.DEFAULT_EXCLUDE_PACKAGES;
            }
            
            if (interval != 0 && interval < ProfilerSetting.MIN_INTERVAL) { //special case for interval 0, it means profiler on standby
                throw new InvalidConfigException("Profiling interval should be >= " + ProfilerSetting.MIN_INTERVAL + " but found " + interval);
            }
            
            int ciruitBreakerDurationThreshold = jsonObject.has(CIRCUIT_BREAKER_DURATION_THRESHOLD) ? jsonObject.getInt(CIRCUIT_BREAKER_DURATION_THRESHOLD) : ProfilerSetting.DEFAULT_CIRCUIT_BREAKER_DURATION_THRESHOLD;
            int circuitBreakerCountThreshold = jsonObject.has(CIRCUIT_BREAKER_COUNT_THRESHOLD) ? jsonObject.getInt(CIRCUIT_BREAKER_COUNT_THRESHOLD) : ProfilerSetting.DEFAULT_CIRCUIT_BREAKER_COUNT_THRESHOLD;
            
            return new ProfilerSetting(isEnabled, excludePackages, interval, ciruitBreakerDurationThreshold, circuitBreakerCountThreshold);
        } catch (JSONException e) {
            throw new InvalidConfigException("Failed parsing profiler settings from config file: " + e.getMessage(), e);
        } 
    }

}
