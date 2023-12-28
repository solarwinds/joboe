package com.solarwinds.joboe.core.config;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.solarwinds.joboe.core.TraceConfig;
import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Container that stores {@link TraceConfig} mapped by URL
 *
 * @author pluk
 */
public class TraceConfigs implements Serializable {
    private final Logger logger = LoggerFactory.getLogger();

    private final Map<ResourceMatcher, TraceConfig> traceConfigsByMatcher;

    private final Cache<String, TraceConfig> lruCache = CacheBuilder.newBuilder()
            .recordStats()
            .maximumSize(1048)
            .build();

    private final Cache<String, String> lruCacheKey = CacheBuilder.newBuilder()
            .recordStats()
            .maximumSize(1048)
            .build();


    public TraceConfigs(Map<ResourceMatcher, TraceConfig> traceConfigsByMatcher) {
        this.traceConfigsByMatcher = traceConfigsByMatcher;
    }

    public TraceConfig getTraceConfig(List<String> signals) {
        StringBuilder key = new StringBuilder();
        signals.forEach(key::append);
        TraceConfig result = null;

        if (lruCacheKey.getIfPresent(key.toString()) != null) {
            return lruCache.getIfPresent(key.toString());
        }

        outer:
        for (Entry<ResourceMatcher, TraceConfig> entry : traceConfigsByMatcher.entrySet()) {
            for (String signal : signals) {
                if (entry.getKey().matches(signal)) {
                    result = entry.getValue();
                    break outer;
                }
            }
        }

        if (result != null) {
            lruCache.put(key.toString(), result);
        }

        logger.debug(lruCache.stats().toString());
        lruCacheKey.put(key.toString(), key.toString());
        return result;
    }
}


