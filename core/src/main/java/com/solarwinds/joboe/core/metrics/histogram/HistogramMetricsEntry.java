package com.solarwinds.joboe.core.metrics.histogram;

import java.util.Collections;
import java.util.Map;

import com.solarwinds.joboe.core.metrics.MetricKey;
import com.solarwinds.joboe.core.metrics.MetricsEntry;

/**
 * A {@link MetricsEntry} that contains a {@link Histogram} as its value
 * @author pluk
 *
 */
public class HistogramMetricsEntry extends MetricsEntry<Histogram>{
    public HistogramMetricsEntry(String name, Map<String, String> tags, Histogram value) {
        this(new MetricKey(name, tags), value);
    }
    
    public HistogramMetricsEntry(MetricKey key, Histogram value) {
        super(key, value);
    }

    @Override
    public MetricsEntryType getType() {
        return MetricsEntryType.HISTOGRAM;
    }

    @Override
    public Map<String,String> getSerializedKvs() {
        return Collections.singletonMap("value", new String(value.encodeBase64()));
    }
    
}
