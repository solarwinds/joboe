package com.solarwinds.joboe.metrics;

import java.util.*;
import java.util.Map.Entry;


import com.solarwinds.joboe.core.metrics.MetricsEntry;
import com.solarwinds.joboe.core.metrics.measurement.SimpleMeasurementMetricsEntry;
import com.solarwinds.joboe.sampling.TraceConfig;
import com.solarwinds.joboe.sampling.TraceDecisionUtil;

/**
 * Sub metrics collector that collects metrics recorded by {@link TraceDecisionUtil} such as "RequestCount", "TraceCount"
 * @author Patson Luk
 *
 */
public class TraceDecisionMetricsCollector extends AbstractMetricsEntryCollector {
   
    @Override
    public List<? extends MetricsEntry<?>> collectMetricsEntries() {
        List<SimpleMeasurementMetricsEntry> layerMetricsEntries = new ArrayList<SimpleMeasurementMetricsEntry>();
        
        
        layerMetricsEntries.add(getMetricEntry(TraceDecisionUtil.MetricType.THROUGHPUT, "RequestCount"));
        layerMetricsEntries.add(getMetricEntry(TraceDecisionUtil.MetricType.TOKEN_BUCKET_EXHAUSTION, "TokenBucketExhaustionCount"));
        layerMetricsEntries.add(getMetricEntry(TraceDecisionUtil.MetricType.TRACE_COUNT, "TraceCount"));
        layerMetricsEntries.add(getMetricEntry(TraceDecisionUtil.MetricType.SAMPLE_COUNT, "SampleCount"));
        layerMetricsEntries.add(getMetricEntry(TraceDecisionUtil.MetricType.THROUGH_TRACE_COUNT, "ThroughTraceCount"));
        layerMetricsEntries.add(getMetricEntry(TraceDecisionUtil.MetricType.TRIGGERED_TRACE_COUNT, "TriggeredTraceCount"));

        Map<String, TraceConfig> layerConfigs = TraceDecisionUtil.consumeLastTraceConfigs();
        
        Map<Entry<String, Object>, Integer> layerSampleRate = new HashMap<Entry<String, Object>, Integer>();
        Map<Entry<String, Object>, Integer> layerSampleSource = new HashMap<Entry<String, Object>, Integer>();
        for (Entry<String, TraceConfig> layerConfig : layerConfigs.entrySet()) {
            layerSampleRate.put(new AbstractMap.SimpleEntry("layer", layerConfig.getKey()), layerConfig.getValue().getSampleRate());
            layerSampleSource.put(new AbstractMap.SimpleEntry("layer", layerConfig.getKey()), layerConfig.getValue().getSampleRateSourceValue());
        }
        
        layerMetricsEntries.addAll(convertToMetricsEntries(layerSampleRate, "SampleRate"));
        layerMetricsEntries.addAll(convertToMetricsEntries(layerSampleSource, "SampleSource"));
        
        return layerMetricsEntries;
    }

    private SimpleMeasurementMetricsEntry getMetricEntry(TraceDecisionUtil.MetricType metricType, String keyName) {
        return new SimpleMeasurementMetricsEntry(keyName, Collections.emptyMap(), TraceDecisionUtil.consumeMetricsData(metricType));
    }
    private List<SimpleMeasurementMetricsEntry> convertToMetricsEntries(Map<Entry<String, Object>, Integer> data, String keyName) {
        List<SimpleMeasurementMetricsEntry> entries = new ArrayList<SimpleMeasurementMetricsEntry>();
        for (Entry<Entry<String, Object>, Integer> metricsEntry : data.entrySet()) {
            Entry<String, Object> singleTag = metricsEntry.getKey();
            Map<String, Object> tags = Collections.singletonMap(singleTag.getKey(), singleTag.getValue());
            entries.add(new SimpleMeasurementMetricsEntry(keyName, tags, metricsEntry.getValue()));
        }
        return entries;
    }
}
