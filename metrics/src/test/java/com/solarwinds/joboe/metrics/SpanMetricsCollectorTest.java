package com.solarwinds.joboe.metrics;

import com.solarwinds.joboe.core.MetricSpanReporter;
import com.solarwinds.joboe.core.metrics.MetricsEntry;
import com.solarwinds.joboe.core.metrics.TopLevelMetricsEntry;
import com.solarwinds.joboe.core.metrics.measurement.SimpleMeasurementMetricsEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private SpanMetricsCollector tested = new SpanMetricsCollector(null);

    @Test
    public void testCollect() {
        final List<MetricsEntry<?>> testMetricEntries = new ArrayList<MetricsEntry<?>>();
        testMetricEntries.add(new SimpleMeasurementMetricsEntry("test-key", 1));
        MetricSpanReporter testSpanReporter = new MetricSpanReporter() {
            @Override
            public List<MetricsEntry<?>> consumeMetricEntries() {
                return testMetricEntries;
            }
        };

        assertEquals(testMetricEntries, tested.collectMetricsEntries(Collections.singleton(testSpanReporter)));
        tested = new SpanMetricsCollector(() -> true);
        List<? extends MetricsEntry<?>> collectedMetricsEntries = tested.collectMetricsEntries(Collections.singleton(testSpanReporter));

        assertEquals(testMetricEntries.size() + 1, collectedMetricsEntries.size()); //+1 as it should have the extra overflow KV
        assertTrue(collectedMetricsEntries.contains(new TopLevelMetricsEntry<Boolean>(SpanMetricsCollector.TRANSACTION_NAME_OVERFLOW_LABEL, true)));
    }

    @Test
    void verifyThatMetricFlushListenerIsInvoked() {
        MetricFlushListener metricFlushListener = mock(MetricFlushListener.class);
        tested.setMetricFlushListener(metricFlushListener);

        tested.collectMetricsEntries();
        verify(metricFlushListener).onFlush();
    }
}
