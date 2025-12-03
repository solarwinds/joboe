package com.solarwinds.joboe.sampling;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

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

    private final Map<ResourceMatcher, TraceConfig> traceConfigsByMatcher;

    private final Cache<String, TraceConfig> lruCache = Caffeine.newBuilder()
            .maximumSize(1048)
            .build();

    private final Cache<String, String> lruCacheKey = Caffeine.newBuilder()
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

        lruCacheKey.put(key.toString(), key.toString());
        return result;
    }
}


