package com.tracelytics.joboe.config;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.joboe.TraceConfig;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;


/**
 * Container that stores {@link TraceConfig} mapped by URL
 * @author pluk
 *
 */
public class TraceConfigs implements Serializable {
    private Logger logger = LoggerFactory.getLogger();
    
    private final Map<ResourceMatcher, TraceConfig> traceConfigsByMatcher;
    
    
    public TraceConfigs(Map<ResourceMatcher, TraceConfig> traceConfigsByMatcher) {
        this.traceConfigsByMatcher = traceConfigsByMatcher;
    }
    
    public TraceConfig getTraceConfig(String resource) {
        //this could be slow and need a cache if so
        for (Entry<ResourceMatcher, TraceConfig> entry : traceConfigsByMatcher.entrySet()) {
            if (entry.getKey().matches(resource)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
}


