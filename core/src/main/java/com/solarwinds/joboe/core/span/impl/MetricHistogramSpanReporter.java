package com.solarwinds.joboe.core.span.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.solarwinds.joboe.core.span.impl.Span.TraceProperty;
import com.solarwinds.joboe.core.metrics.MetricKey;
import com.solarwinds.joboe.core.metrics.MetricsEntry;
import com.solarwinds.joboe.core.metrics.histogram.Histogram;
import com.solarwinds.joboe.core.metrics.histogram.HistogramException;
import com.solarwinds.joboe.core.metrics.histogram.HistogramFactory;
import com.solarwinds.joboe.core.metrics.histogram.HistogramFactory.HistogramType;
import com.solarwinds.joboe.core.metrics.histogram.HistogramMetricsEntry;

/**
 * Records histograms and measurements metrics when span finishes
 * @author pluk
 *
 */
public class MetricHistogramSpanReporter extends MetricSpanReporter {
    public static final String TRANSACTION_LATENCY_METRIC_NAME = "TransactionResponseTime";
    public static final String TRANSACTION_NAME_TAG_KEY = "TransactionName";
    private static final HistogramType HISTOGRAM_TYPE = HistogramType.HDR;
    
    private LoadingCache<MetricKey, Histogram> histograms;
    
    
    public static final MetricHistogramSpanReporter REPORTER = new MetricHistogramSpanReporter();
    
    private MetricHistogramSpanReporter() {
        this.histograms = isSpanMetricsEnabled() ? createHistogramCache() : null;
    }
    
    private static LoadingCache<MetricKey, Histogram> createHistogramCache() {
        return CacheBuilder.newBuilder().build(new CacheLoader<MetricKey, Histogram>() {
                    @Override
                    public Histogram load(MetricKey key) throws Exception {
                        return HistogramFactory.buildHistogram(HISTOGRAM_TYPE, MAX_DURATION);
                    }
                });
    }
    
    /**
     * Records the span duration as a histogram and a measurement
     */
    @Override
    protected void reportMetrics(Span span, long duration) {
        if (!span.isRoot()) { //only insert values to histograms if this is the root span 
            return;
        }
        MetricKey serviceHistogramKey = new MetricKey(TRANSACTION_LATENCY_METRIC_NAME, null); //globally for all transactions within this service
        Histogram serviceHistogram; 
        serviceHistogram = histograms.getUnchecked(serviceHistogramKey);
        
        try {
            serviceHistogram.recordValue(duration);
        } catch (HistogramException e) {
            logger.debug("Failed to report metrics to service level histogram : " + e.getMessage(), e);
        }
        
        String transactionName = span.getTracePropertyValue(TraceProperty.TRANSACTION_NAME);
        
        if (transactionName != null) {
            MetricKey transactionHistogramKey = new MetricKey(TRANSACTION_LATENCY_METRIC_NAME, Collections.singletonMap("TransactionName", transactionName)); //specifically for this transaction
            Histogram transactionHistogram = histograms.getUnchecked(transactionHistogramKey);
            try {
                transactionHistogram.recordValue(duration);
            } catch (HistogramException e) {
                logger.debug("Failed to report metrics to transaction histogram with metrics key [" + transactionHistogramKey + "] : " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Consumes and resets metric entries on this reporter 
     * @return  a list of metric entries collected so since previous call to this method
     */
    @Override
    public List<MetricsEntry<?>> consumeMetricEntries() {
        Map<MetricKey, Histogram> reportingHistograms = consumeHistograms();
        
        List<MetricsEntry<?>> entries = new ArrayList<MetricsEntry<?>>();
        for (Entry<MetricKey, Histogram> entry : reportingHistograms.entrySet()) {
            entries.add(new HistogramMetricsEntry(entry.getKey(), entry.getValue()));
        }
        
        return entries;
    }
    
    public Map<MetricKey, Histogram> consumeHistograms() {
        Map<MetricKey, Histogram> reportingHistograms = histograms.asMap();
        histograms = createHistogramCache();
        
        return reportingHistograms;
    }
}