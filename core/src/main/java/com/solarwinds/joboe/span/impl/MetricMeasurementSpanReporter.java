package com.solarwinds.joboe.span.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.solarwinds.metrics.MetricKey;
import com.solarwinds.metrics.MetricsEntry;
import com.solarwinds.metrics.measurement.SummaryLongMeasurement;
import com.solarwinds.metrics.measurement.SummaryMeasurementMetricsEntry;

/**
 * Records histograms and measurements metrics when span finishes
 * @author pluk
 *
 */
public abstract class MetricMeasurementSpanReporter extends MetricSpanReporter {
    public static final String TRANSACTION_NAME_TAG_KEY = "TransactionName";
    public static final String HAS_ERROR_TAG_KEY = "Errors";
    
    
    private LoadingCache<MetricKey, SummaryLongMeasurement> measurements;
    private final String measurementName;
    
    //public static final MetricMeasurementSpanReporter REPORTER = new MetricMeasurementSpanReporter();
    
    protected MetricMeasurementSpanReporter(String measurementName) {
        this.measurements = isSpanMetricsEnabled() ? createMeasurementCache() : null;
        this.measurementName = measurementName;
    }
    
    private static LoadingCache<MetricKey, SummaryLongMeasurement> createMeasurementCache() {
        return CacheBuilder.newBuilder().build(new CacheLoader<MetricKey, SummaryLongMeasurement>() {
                    @Override
                    public SummaryLongMeasurement load(MetricKey key) throws Exception {
                        return new SummaryLongMeasurement();
                    }
                }); 
    }

    protected void recordMeasurementEntry(Map<String, String> primaryKeys, long duration) {
        recordMeasurementEntry(primaryKeys, null, duration);
    }
    
    protected void recordMeasurementEntry(Map<String, String> primaryKeys, Map<String, String> optionalKeys, long duration) {
        MetricKey measurementKey = new MetricKey(measurementName, new HashMap<String, String>(primaryKeys));
        measurements.getUnchecked(measurementKey).recordValue(duration);
        
        if (optionalKeys != null) {
            for (Entry<String, String> optionalKey : optionalKeys.entrySet()) {
                Map<String, String> tags = new HashMap<String, String>(primaryKeys);
                tags.put(optionalKey.getKey(), optionalKey.getValue());
                measurementKey = new MetricKey(measurementName, tags);
                measurements.getUnchecked(measurementKey).recordValue(duration);
            }
        }
    }
    
    /**
     * Consumes and resets metric entries on this reporter 
     * @return  a list of metric entries collected so since previous call to this method
     */
    public List<MetricsEntry<?>> consumeMetricEntries() {
        Map<MetricKey, SummaryLongMeasurement> reportingMeasurements = consumeMeasurements();
        
        List<MetricsEntry<?>> entries = new ArrayList<MetricsEntry<?>>();
        
        for (Entry<MetricKey, SummaryLongMeasurement> entry : reportingMeasurements.entrySet()) {
            entries.add(new SummaryMeasurementMetricsEntry(entry.getKey(), entry.getValue()));
        }
        
        return entries;
    }

    
    public Map<MetricKey, SummaryLongMeasurement> consumeMeasurements() {
        Map<MetricKey, SummaryLongMeasurement> consumedMeasurements = this.measurements.asMap();
        this.measurements = createMeasurementCache();
        
        return consumedMeasurements;
    }
}