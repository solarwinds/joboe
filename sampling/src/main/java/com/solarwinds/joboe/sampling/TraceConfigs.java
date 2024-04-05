package com.solarwinds.joboe.sampling;


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

    private final Map<String, TraceConfig> lruCache = new LruCache<>(1048);

    private final Map<String, String> lruCacheKey = new LruCache<>(1048);


    public TraceConfigs(Map<ResourceMatcher, TraceConfig> traceConfigsByMatcher) {
        this.traceConfigsByMatcher = traceConfigsByMatcher;
    }

    public TraceConfig getTraceConfig(List<String> signals) {
        StringBuilder key = new StringBuilder();
        signals.forEach(key::append);
        TraceConfig result = null;

        if (lruCacheKey.get(key.toString()) != null) {
            return lruCache.get(key.toString());
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


