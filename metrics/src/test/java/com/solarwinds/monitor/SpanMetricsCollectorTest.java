package com.solarwinds.monitor;

import com.solarwinds.joboe.span.impl.MetricSpanReporter;
import com.solarwinds.joboe.span.impl.Span;
import com.solarwinds.joboe.span.impl.TransactionNameManager;
import com.solarwinds.metrics.MetricsEntry;
import com.solarwinds.metrics.TopLevelMetricsEntry;
import com.solarwinds.metrics.measurement.SimpleMeasurementMetricsEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test collecting metrics from Span reporter/actor and the "TransactionNameOverflow" flag
 * @author pluk
 *
 */
public class SpanMetricsCollectorTest {
    private final SpanMetricsCollector tested = new SpanMetricsCollector();

    @BeforeEach
    protected void setUp() throws Exception {
        TransactionNameManager.clearTransactionNames();
    }

    @Test
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

        assertEquals(testMetricEntries, tested.collectMetricsEntries(Collections.singleton(testSpanReporter)));
        
        //now test the extra transaction name limit kv 
        for (int i = 0 ; i < TransactionNameManager.DEFAULT_MAX_NAME_COUNT + 1; i++) {
            TransactionNameManager.addTransactionName(String.valueOf(i)); //trigger overflow
        }

        List<? extends MetricsEntry<?>> collectedMetricsEntries = tested.collectMetricsEntries(Collections.singleton(testSpanReporter));
        assertEquals(testMetricEntries.size() + 1, collectedMetricsEntries.size()); //+1 as it should have the extra overflow KV
        assertTrue(collectedMetricsEntries.contains(new TopLevelMetricsEntry<Boolean>(SpanMetricsCollector.TRANSACTION_NAME_OVERFLOW_LABEL, true)));
        
        //collect again, that overflow flag should be reset
        assertEquals(testMetricEntries, tested.collectMetricsEntries(Collections.singleton(testSpanReporter)));
    }

    @Test
    void verifyThatMetricFlushListenerIsInvoked() {
        MetricFlushListener metricFlushListener = mock(MetricFlushListener.class);
        tested.setMetricFlushListener(metricFlushListener);

        tested.collectMetricsEntries();
        verify(metricFlushListener).onFlush();
    }
}
