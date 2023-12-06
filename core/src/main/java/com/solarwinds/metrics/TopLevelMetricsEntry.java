package com.solarwinds.metrics;

import java.util.Collections;
import java.util.Map;

/**
 * Top level Metrics Entry that does not belong to Measurement nor Histogram
 * @author pluk
 *
 * @param <T>
 */
public class TopLevelMetricsEntry<T> extends MetricsEntry<T> {
    public TopLevelMetricsEntry(String key, T value) {
        super(new MetricKey(key, null), value);
    }
    
    @Override
    public Map<String, ?> getSerializedKvs() {
        return Collections.singletonMap(getKey().getStringKey(), value);
    }

    @Override
    public MetricsEntryType getType() {
        return MetricsEntryType.TOP_LEVEL;
    }

}
