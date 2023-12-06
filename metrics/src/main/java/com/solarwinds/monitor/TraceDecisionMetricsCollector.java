package com.solarwinds.monitor;

import java.util.*;
import java.util.Map.Entry;

import com.solarwinds.joboe.TraceDecisionUtil;
import com.solarwinds.joboe.TraceDecisionUtil.MetricType;
import com.solarwinds.joboe.TraceConfig;
import com.solarwinds.metrics.MetricsEntry;
import com.solarwinds.metrics.measurement.SimpleMeasurementMetricsEntry;

/**
 * Sub metrics collector that collects metrics recorded by {@link TraceDecisionUtil} such as "RequestCount", "TraceCount"
 * @author Patson Luk
 *
 */
class TraceDecisionMetricsCollector extends AbstractMetricsEntryCollector {
   
    @Override
    List<? extends MetricsEntry<?>> collectMetricsEntries() {
        List<SimpleMeasurementMetricsEntry> layerMetricsEntries = new ArrayList<SimpleMeasurementMetricsEntry>();
        
        
        layerMetricsEntries.add(getMetricEntry(MetricType.THROUGHPUT, "RequestCount"));
        layerMetricsEntries.add(getMetricEntry(MetricType.TOKEN_BUCKET_EXHAUSTION, "TokenBucketExhaustionCount"));
        layerMetricsEntries.add(getMetricEntry(MetricType.TRACE_COUNT, "TraceCount"));
        layerMetricsEntries.add(getMetricEntry(MetricType.SAMPLE_COUNT, "SampleCount"));
        layerMetricsEntries.add(getMetricEntry(MetricType.THROUGH_TRACE_COUNT, "ThroughTraceCount"));
        layerMetricsEntries.add(getMetricEntry(MetricType.TRIGGERED_TRACE_COUNT, "TriggeredTraceCount"));

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

    private SimpleMeasurementMetricsEntry getMetricEntry(MetricType metricType, String keyName) {
        return new SimpleMeasurementMetricsEntry(keyName, Collections.<String, Object>emptyMap(), TraceDecisionUtil.consumeMetricsData(metricType));
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
