package com.solarwinds.joboe.span.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.solarwinds.joboe.span.impl.Span.TraceProperty;

public class TracePropertyDictionary {
    private static final LoadingCache<Long, Map<TraceProperty<?>, Object>> allTraceProperties = CacheBuilder.newBuilder().expireAfterWrite(1200, TimeUnit.SECONDS).<Long, Map<TraceProperty<?>, Object>>build(
                new CacheLoader<Long, Map<TraceProperty<?>, Object>>() {
                    public Map<TraceProperty<?>, Object> load(Long key) {
                      return new HashMap<TraceProperty<?>, Object>();
                    }
                }
            ); //20 mins cache
    
    private TracePropertyDictionary() {
        
    }
    
    public static Map<TraceProperty<?>, Object> getTracePropertiesByTraceId(long traceId) {
        return allTraceProperties.getUnchecked(traceId);
    }
    
    public static void removeTracePropertiesByTraceId(long traceId) {
        allTraceProperties.invalidate(traceId);
    }
}
