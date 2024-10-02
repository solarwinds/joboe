package com.tracelytics.monitor.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tracelytics.joboe.JoboeTest;
import com.tracelytics.joboe.span.impl.MetricSpanReporter;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.TransactionNameManager;
import com.tracelytics.metrics.MetricsEntry;
import com.tracelytics.metrics.TopLevelMetricsEntry;
import com.tracelytics.metrics.measurement.SimpleMeasurementMetricsEntry;

/**
 * Test collecting metrics from Span reporter/actor and the "TransactionNameOverflow" flag
 * @author pluk
 *
 */
public class SpanMetricsCollectorTest extends JoboeTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TransactionNameManager.clearTransactionNames();
    }
    
    public void testCollect() throws Exception {
        final List<MetricsEntry<?>> testMetricEntries = new ArrayList<MetricsEntry<?>>();
        testMetricEntries.add(new SimpleMeasurementMetricsEntry("test-key", 1));
        
        MetricSpanReporter testSpanReporter  =new MetricSpanReporter() {
            @Override
            protected void reportMetrics(Span span, long duration) {
            }
            
            @Override
            public List<MetricsEntry<?>> consumeMetricEntries() {
                return testMetricEntries;
            }
        };
        
        SpanMetricsCollector collector = new SpanMetricsCollector();
        assertEquals(testMetricEntries, collector.collectMetricsEntries(Collections.singleton(testSpanReporter)));
        
        //now test the extra transaction name limit kv 
        for (int i = 0 ; i < TransactionNameManager.DEFAULT_MAX_NAME_COUNT + 1; i++) {
            TransactionNameManager.addTransactionName(String.valueOf(i)); //trigger overflow
        }
        
        List<? extends MetricsEntry<?>> collectedMetricsEntries = collector.collectMetricsEntries(Collections.singleton(testSpanReporter));
        assertEquals(testMetricEntries.size() + 1, collectedMetricsEntries.size()); //+1 as it should have the extra overflow KV
        assertTrue(collectedMetricsEntries.contains(new TopLevelMetricsEntry<Boolean>(SpanMetricsCollector.TRANSACTION_NAME_OVERFLOW_LABEL, true)));
        
        //collect again, that overflow flag should be reset
        assertEquals(testMetricEntries, collector.collectMetricsEntries(Collections.singleton(testSpanReporter)));
    }
}
