package com.solarwinds.joboe.metrics;

import com.solarwinds.joboe.core.metrics.measurement.MeasurementMetricsEntry;
import com.solarwinds.joboe.core.metrics.measurement.SimpleMeasurementMetricsEntry;
import com.solarwinds.joboe.core.metrics.measurement.SummaryDoubleMeasurement;
import com.solarwinds.joboe.core.metrics.measurement.SummaryMeasurement;
import com.solarwinds.joboe.core.metrics.measurement.SummaryMeasurementMetricsEntry;
import com.solarwinds.joboe.sampling.SettingsArg;
import com.solarwinds.joboe.sampling.SettingsArgChangeListener;
import com.solarwinds.joboe.sampling.SettingsManager;
import com.solarwinds.joboe.shaded.google.common.cache.CacheBuilder;
import com.solarwinds.joboe.shaded.google.common.cache.CacheLoader;
import com.solarwinds.joboe.shaded.google.common.cache.LoadingCache;
import com.solarwinds.joboe.core.metrics.MetricKey;
import com.solarwinds.joboe.core.metrics.MetricsEntry;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

/**
 * Sub metrics collector that collects custom metrics, it serves 2 purposes:
 * 1. Allow api trigger to report custom metrics and it gets batched up here
 * 2. Allow metrics collector to periodically call this to collect metrics recorded in point 1
 *  
 * @author Patson Luk
 *
 */
public class CustomMetricsCollector extends AbstractMetricsEntryCollector {
    public static final CustomMetricsCollector INSTANCE = new CustomMetricsCollector();
    private LoadingCache<MetricKey, SummaryDoubleMeasurement> summaryMeasurements;
    private LoadingCache<MetricKey, Long> incrementMeasurements;
    
    private static final int DEFAULT_LIMIT = 500;
    static int limit = DEFAULT_LIMIT;
    private boolean reportedLimitExceeded = false;
    static final int TAGS_LIMIT = 50;
    
    static {
        addLimitChangeListener();
    }
    
    private CustomMetricsCollector() {
        reset();
        limit = DEFAULT_LIMIT;
        reportedLimitExceeded = false;
    }
    
    private static void addLimitChangeListener() {
        SettingsManager.registerListener(new SettingsArgChangeListener<Integer>(SettingsArg.MAX_CUSTOM_METRICS) {
            @Override
            public void onChange(Integer newValue) {
                if (newValue != null) {
                    limit = newValue;
                } else {
                    limit = DEFAULT_LIMIT;
                }
            }
        });
        
    }

    
    @Override
    List<? extends MetricsEntry<?>> collectMetricsEntries() {
        List<MeasurementMetricsEntry<?>> entries = new ArrayList<MeasurementMetricsEntry<?>>();
        
        ConcurrentMap<MetricKey, SummaryDoubleMeasurement> reportingSummaryMeasurements = summaryMeasurements.asMap();
        ConcurrentMap<MetricKey, Long> reportingIncrementMeasurements = incrementMeasurements.asMap();
        
        reset();
        
        for (Entry<MetricKey, SummaryDoubleMeasurement> entry : reportingSummaryMeasurements.entrySet()) {
            entries.add(new SummaryMeasurementMetricsEntry(entry.getKey(), entry.getValue()));
        }
        
        for (Entry<MetricKey, Long> entry : reportingIncrementMeasurements.entrySet()) {
            entries.add(new SimpleMeasurementMetricsEntry(entry.getKey(), entry.getValue()) {
                @Override
                /**
                 * Overrides to give a different label "count" instead of "value"
                 */
                public Map<String, Number> getSerializedKvs() {
                    return Collections.singletonMap("count", value);
                }
            });
        }
        return entries;
    }
    
    /**
     * For internal testing only
     * @param name
     * @param tags
     * @return
     */
    public Double getSum(String name, Map<String, String> tags) {
        MetricKey key = new MetricKey(name, tags);
        SummaryDoubleMeasurement summaryMeasurement = summaryMeasurements.getIfPresent(key);
        if (summaryMeasurement != null) {
            return summaryMeasurement.getSum();
        } else {
            return null;
        }
    }
    
    /**
     * For internal testing only
     * @param name
     * @param tags
     * @return  
     */
    public Long getCount(String name, Map<String, String> tags) {
        MetricKey key = new MetricKey(name, tags);
        SummaryDoubleMeasurement summaryMeasurement = summaryMeasurements.getIfPresent(key);
        if (summaryMeasurement != null) {
            return summaryMeasurement.getCount();
        }
        return incrementMeasurements.getIfPresent(key);
    }
    
    public final void reset() {
        summaryMeasurements = createSummaryMeasurementCache();
        incrementMeasurements = createIncrementMeasurementCache();
    }
    
    private static LoadingCache<MetricKey, SummaryDoubleMeasurement> createSummaryMeasurementCache() {
        return CacheBuilder.newBuilder().build(new CacheLoader<MetricKey, SummaryDoubleMeasurement>() {
                    @Override
                    public SummaryDoubleMeasurement load(MetricKey key) throws Exception {
                        return new SummaryDoubleMeasurement();
                    }
                }); 
    }
    
    private static LoadingCache<MetricKey, Long> createIncrementMeasurementCache() {
        return CacheBuilder.newBuilder().build(new CacheLoader<MetricKey, Long> () {
                    @Override
                    public Long load(MetricKey key) throws Exception {
                        return 0L;
                    }
                }); 
    }
    
    
    public void recordSummaryMetric(String name, double value, int count, Map<String, String> tags) {
        if (tags != null && tags.size() > TAGS_LIMIT) {
            logger.warn("Only " + TAGS_LIMIT + " tags are allowed but found " + tags.size() + " tags in metric [" + name + "], entry ignored");
            return;
        }

        if (count <= 0) {
            logger.warn("Only positive count is allowed but found " + count + " in metric [" + name + "], entry ignored");
            return;
        }
        
        MetricKey metricKey = new MetricKey(name, tags);
        
        if (summaryMeasurements.getIfPresent(metricKey) == null && summaryMeasurements.size() + incrementMeasurements.size() >= limit) {
            if (!reportedLimitExceeded) {
                logger.warn("Dropping metric entry with name [" + name + "] as limit " + limit + " has been reached. No more metric entry with new name/tags will be accepted until next report cycle...");
                reportedLimitExceeded = true;
            }
            return;
        }
        
        SummaryMeasurement<Double> measurement = summaryMeasurements.getUnchecked(metricKey);
        measurement.recordValue(value);
        if (count != 1) {
            measurement.incrementCount(count - 1);
        }
        
    }
    
    public void recordIncrementMetrics(String name, int count, Map<String, String> tags) {
        if (tags != null && tags.size() > TAGS_LIMIT) {
            logger.warn("Only " + TAGS_LIMIT + " tags are allowed but found " + tags.size() + " tags in metric [" + name + "], entry ignored");
            return;
        }

        if (count <= 0) {
            logger.warn("Only positive count is allowed but found " + count + " in metric [" + name + "], entry ignored");
            return;
        }
        
        MetricKey metricKey = new MetricKey(name, tags);
        
        if (incrementMeasurements.getIfPresent(metricKey) == null && summaryMeasurements.size() + incrementMeasurements.size() >= limit) {
            if (!reportedLimitExceeded) {
                logger.warn("Dropping metric entry with name [" + name + "] as limit " + limit + " has been reached. No more metric entry with new name/tags will be accepted until next report cycle...");
                reportedLimitExceeded = true;
            }
            return;
        }
        
        Long measurement = incrementMeasurements.getUnchecked(metricKey);
        incrementMeasurements.put(metricKey, measurement + count);
    }
 
}
