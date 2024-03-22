package com.solarwinds.joboe.core.span.impl;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.core.Context;
import com.solarwinds.joboe.core.metrics.MetricKey;
import com.solarwinds.joboe.core.metrics.histogram.Histogram;
import com.solarwinds.joboe.core.metrics.measurement.SummaryLongMeasurement;
import com.solarwinds.joboe.core.settings.TestSettingsReader;
import com.solarwinds.joboe.core.span.impl.Span.SpanProperty;
import com.solarwinds.joboe.core.span.impl.Span.TraceProperty;
import com.solarwinds.joboe.core.span.impl.Tracer.SpanBuilder;
import com.solarwinds.joboe.core.util.TestUtils;
import com.solarwinds.joboe.sampling.TraceDecisionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InboundMetricSpanReporterTest {
    protected static final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();
    
    @BeforeEach
    protected void setUp() throws Exception {
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
        TraceDecisionUtil.reset();
        ConfigManager.reset();
        
        testSettingsReader.reset();
        testSettingsReader.put(TestUtils.getDefaultSettings());
        
        MetricHistogramSpanReporter.REPORTER.consumeMetricEntries(); //clear data
        InboundMetricMeasurementSpanReporter.REPORTER.consumeMetricEntries(); //clear data
        TransactionNameManager.clearTransactionNames();
    }
    
    private SpanBuilder getSpanBuilder(String operationName) {
        return Tracer.INSTANCE.buildSpan(operationName).withReporters(MetricHistogramSpanReporter.REPORTER, InboundMetricMeasurementSpanReporter.REPORTER).withSpanProperty(SpanProperty.TRACE_DECISION_PARAMETERS, new TraceDecisionParameters(Collections.EMPTY_MAP, null));
    }
    
    /**
     * Test operation with the same name
     * @throws Exception
     */
    @Test
    public void testSimpleSpan1() throws Exception {
        final int RUN_COUNT = 100;
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            Span span = getSpanBuilder("simple").start(); 
            span.finish();
        }
        
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        
        Histogram histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, null)); //service level
        assertEquals(RUN_COUNT, histogram.getTotalCount());
        
        histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME
                                               , Collections.singletonMap(MetricMeasurementSpanReporter.TRANSACTION_NAME_TAG_KEY, TransactionNameManager.UNKNOWN_TRANSACTION_NAME))); //transaction level, transaction name "unknown"
        assertEquals(RUN_COUNT, histogram.getTotalCount());
    }

    @Test
    public void testSimpleSpanWithTimestamp() throws Exception {
        final long START_TIME = 10L;
        final long END_TIME = 20L;
        final int RUN_COUNT = 100;
        
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            Span span = getSpanBuilder("with-time-stamp").withStartTimestamp(START_TIME).start(); 
            span.finish(END_TIME);
        }
        
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        
        Histogram histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, null)); //service level
        
        assertEquals(RUN_COUNT, histogram.getTotalCount());
        assertEquals((Long)(END_TIME - START_TIME), histogram.getLast());
        
        histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME
                                               , Collections.singletonMap(MetricMeasurementSpanReporter.TRANSACTION_NAME_TAG_KEY, TransactionNameManager.UNKNOWN_TRANSACTION_NAME))); //transaction level, transaction name "unknown"
        assertEquals(RUN_COUNT, histogram.getTotalCount());
        assertEquals((Long)(END_TIME - START_TIME), histogram.getLast());
        
        Map<MetricKey, SummaryLongMeasurement> measurements = InboundMetricMeasurementSpanReporter.REPORTER.consumeMeasurements();
        assertEquals(1, measurements.size()); //1 entry with TransactionName as unknown
        
        SummaryLongMeasurement measurement = measurements.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME
                                                        , Collections.singletonMap(MetricMeasurementSpanReporter.TRANSACTION_NAME_TAG_KEY, TransactionNameManager.UNKNOWN_TRANSACTION_NAME))); //transaction level, transaction name "unknown"
        assertEquals(RUN_COUNT, measurement.getCount());
    }
    
    /**
     * Hits same endpoint with same controller/action/Status/Method
     * @throws Exception
     */
    @Test
    public void testSpanWithActionController1() throws Exception {
        final int RUN_COUNT = 100;
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            Span span = getSpanBuilder("simple").start();
            span.setTracePropertyValue(TraceProperty.ACTION, "a");
            span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
            span.setTag("Status", 200);
            span.setTag("HTTPMethod", "GET");
            
            span.finish();
        }
        
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        
        Histogram histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, null));
        assertEquals(RUN_COUNT, histogram.getTotalCount());
        histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, Collections.singletonMap(MetricMeasurementSpanReporter.TRANSACTION_NAME_TAG_KEY, "c.a"))); //should only be 1 histogram for this action/controller combo
        assertEquals(RUN_COUNT, histogram.getTotalCount());
        
        Map<MetricKey, SummaryLongMeasurement> measurements = InboundMetricMeasurementSpanReporter.REPORTER.consumeMeasurements();
       //(1 transaction name with no extra tag) + (1 transaction name with status) + (1 transaction name with method)
        assertEquals(3, measurements.size());
        
        SummaryLongMeasurement measurement;
        
        Map<String, String> tags;
        
        tags = new HashMap<String, String>();
        tags.put(MetricMeasurementSpanReporter.TRANSACTION_NAME_TAG_KEY, "c.a");
        tags.put("HttpStatus", "200");
        measurement = measurements.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, tags)); 
        assertEquals(RUN_COUNT, measurement.getCount());
        
        tags = new HashMap<String, String>();
        tags.put(MetricMeasurementSpanReporter.TRANSACTION_NAME_TAG_KEY, "c.a");
        tags.put("HttpMethod", "GET");
        measurement = measurements.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, tags));
        assertEquals(RUN_COUNT, measurement.getCount());
    }
    
    /**
     * Hits different endpoints with different controller/action/status/method
     * @throws Exception
     */
    @Test
    public void testSpanWithActionController2() throws Exception {
        final int RUN_COUNT = 100;
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            Span span = getSpanBuilder("simple").start();
            span.setTracePropertyValue(TraceProperty.ACTION, "a" + i);
            span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
            span.setTag("Status", i + 450); //so both some 4xx and 5xx
            span.setTag("HTTPMethod", "GET" + i);
            span.finish();
        }
        
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        assertEquals(RUN_COUNT + 1, histograms.size()); //RUN_COUNT transaction level + 1 overall
        
        
        Map<MetricKey, SummaryLongMeasurement> measurements = InboundMetricMeasurementSpanReporter.REPORTER.consumeMeasurements();
        //(# transactions no extra tag) + (# transactions with status) + (# transactions with method) + (# transactions with error)
        
        assertEquals(RUN_COUNT + RUN_COUNT + RUN_COUNT + 50, measurements.size()); //50 for Errors for status code from 500 - 549 
    }
    
    /**
     * Hits same endpoint with same URL
     * @throws Exception
     */
    @Test
    public void testSpanWithURL1() throws Exception {
        final int RUN_COUNT = 100;
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            Span span = getSpanBuilder("simple").start();
            span.setTag("HTTP-Host", "localhost");
            span.setTag("URL", "/1/2/3");
            span.finish();
        }
        
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        
        Histogram histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, null));
        assertEquals(RUN_COUNT, histogram.getTotalCount());
        histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, Collections.singletonMap(MetricMeasurementSpanReporter.TRANSACTION_NAME_TAG_KEY, "/1/2"))); //should only be 1 histogram for this URL
        assertEquals(RUN_COUNT, histogram.getTotalCount());
        
        Map<MetricKey, SummaryLongMeasurement> measurements = InboundMetricMeasurementSpanReporter.REPORTER.consumeMeasurements();
        assertEquals(1, measurements.size()); 
    }
    
    /**
     * Hits different endpoint with different URL
     * @throws Exception
     */
    @Test
    public void testSpanWithURL2() throws Exception {
        final int RUN_COUNT = 100;
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            Span span = getSpanBuilder("simple").start();
            span.setTag("HTTP-Host", "localhost");
            span.setTag("URL", "/" + i + "/2/3");
            span.finish();
        }
        
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        
        assertEquals(1 + RUN_COUNT, histograms.size()); //1 service level + RUN_COUNT transaction level
        
        Histogram histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, null));
        assertEquals(RUN_COUNT, histogram.getTotalCount()); //global service level
        
        Map<MetricKey, SummaryLongMeasurement> measurements = InboundMetricMeasurementSpanReporter.REPORTER.consumeMeasurements();
        assertEquals(RUN_COUNT, measurements.size()); //each run has different transactionName
    }
    
    /**
     * Hits same endpoint with controller action but different URL
     * @throws Exception
     */
    @Test
    public void testSpanWithControllerActionAndURL() throws Exception {
        final int RUN_COUNT = 1000;
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            Span span = getSpanBuilder("simple").start();
            span.setTracePropertyValue(TraceProperty.ACTION, "a");
            span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
            span.setTag("HTTP-Host", "localhost");
            span.setTag("URL", "/1/2/3");
            span.finish();
        }
        
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        
        Histogram histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, null));
        assertEquals(RUN_COUNT, histogram.getTotalCount());
        histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, Collections.singletonMap(MetricMeasurementSpanReporter.TRANSACTION_NAME_TAG_KEY, "c.a"))); //controller/action pair has higher precedence over URL
        assertEquals(RUN_COUNT, histogram.getTotalCount());
        
        Map<MetricKey, SummaryLongMeasurement> measurements = InboundMetricMeasurementSpanReporter.REPORTER.consumeMeasurements();
        assertEquals(1, measurements.size()); //1 entry for TransactionName = "a.c"
    }

    @Test
    public void testTransactionNameLimit() throws Exception {
        final int RUN_COUNT = 1000;
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            Span span = getSpanBuilder("simple").start();
            span.setTracePropertyValue(TraceProperty.ACTION, "a" + i); //so generate 1000 transaction name
            span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
            span.setTag("Status", i + 450); //so both some 4xx and 5xx
            span.setTag("HTTPMethod", "GET" + i);
            span.finish();
        }
        
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        assertEquals(1 + TransactionNameManager.DEFAULT_MAX_NAME_COUNT + 1, histograms.size()); //1 service level + transaction level (max transaction name allowed) + 1 "other"
        
        Histogram histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, null));
        assertEquals(RUN_COUNT, histogram.getTotalCount()); //global service level
        
        Map<MetricKey, SummaryLongMeasurement> measurements = InboundMetricMeasurementSpanReporter.REPORTER.consumeMeasurements();
        //(# transactions @ DEFAULT_MAX_TRANSACION_NAME_COUNT no extra tag) +   
        //(# transactions @ DEFAULT_MAX_TRANSACION_NAME_COUNT with status) +   
        //(# transactions @ DEFAULT_MAX_TRANSACION_NAME_COUNT with method) + 
        //(# transactions @ DEFAULT_MAX_TRANSACION_NAME_COUNT with error) +
        //(# other transactions no extra tag) +
        //(# other transactions with status) +
        //(# other transactions with method) +
        //(# other transactions with error) +
        
        assertEquals(TransactionNameManager.DEFAULT_MAX_NAME_COUNT +
                        TransactionNameManager.DEFAULT_MAX_NAME_COUNT +
                        TransactionNameManager.DEFAULT_MAX_NAME_COUNT +
                        100 + //has error 500-599, so 100 of them
                        1 +
                        RUN_COUNT - TransactionNameManager.DEFAULT_MAX_NAME_COUNT +
                        RUN_COUNT - TransactionNameManager.DEFAULT_MAX_NAME_COUNT, //600+ status code not considered as error
                     measurements.size());  
    }
    
    /**
     * Verifies that invalid durations but exception should not be thrown by reporter
     * @throws Exception
     */
    @Test
    public void testSpanWithInvalidDurations() throws Exception {
        Span span;
        
        long now = System.currentTimeMillis() * 1000; //actually doesn't really matter as it's relative to finish time, even 0 would work...
        
        span = getSpanBuilder("simple").withStartTimestamp(now).start();
        span.finish(now); //valid duration
        
        span = getSpanBuilder("simple").withStartTimestamp(now).start();
        span.finish(now + MetricSpanReporter.MAX_DURATION); //valid duration
        
        span = getSpanBuilder("simple").withStartTimestamp(now).start();
        span.finish(now + MetricSpanReporter.MAX_DURATION * 2); //invalid duration but reporter should not throw exception
        
        span = getSpanBuilder("simple").withStartTimestamp(now).start();
        span.finish(now - 1); //invalid duration but reporter should not throw exception
        
        
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        
        assertEquals(2, histograms.size()); //1 service level + 1 transaction level (transaction name "unknown")
        
        Histogram histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, null));
        assertEquals(2, histogram.getTotalCount()); //global service level - only 2 durations are valid
        
        Map<MetricKey, SummaryLongMeasurement> measurements = InboundMetricMeasurementSpanReporter.REPORTER.consumeMeasurements();
        assertEquals(1, measurements.size());
        SummaryLongMeasurement measurement = measurements.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, Collections.singletonMap("TransactionName", TransactionNameManager.UNKNOWN_TRANSACTION_NAME)));
        assertEquals(2, measurement.getCount()); //only 2 durations are valid
        assertEquals(Long.valueOf(MetricSpanReporter.MAX_DURATION), measurement.getSum()); //only 2 durations are valid
    }
    
    /**
     * Test SDK span with Status, it should NOT flag error even if it's 5xx
     * @throws Exception
     */
    @Test
    public void testSdkSpan() throws Exception {
        final int RUN_COUNT = 1000;
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            Span span = getSpanBuilder("simple").start();
            span.setTracePropertyValue(TraceProperty.ACTION, "a" + i); //so generate 1000 transaction name
            span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
            span.setTag("Status", i + 450); //so both some 4xx and 5xx
            span.setTag("HTTPMethod", "GET" + i);
            span.setSpanPropertyValue(SpanProperty.IS_SDK, true);
            span.finish();
        }
        
        Map<MetricKey, Histogram> histograms = MetricHistogramSpanReporter.REPORTER.consumeHistograms();
        assertEquals(1 + TransactionNameManager.DEFAULT_MAX_NAME_COUNT + 1, histograms.size()); //1 service level + transaction level (max transaction name allowed) + 1 "other"
        
        Histogram histogram = histograms.get(new MetricKey(InboundMetricMeasurementSpanReporter.TRANSACTION_LATENCY_METRIC_NAME, null));
        assertEquals(RUN_COUNT, histogram.getTotalCount()); //global service level
        
        Map<MetricKey, SummaryLongMeasurement> measurements = InboundMetricMeasurementSpanReporter.REPORTER.consumeMeasurements();
   
        assertEquals(TransactionNameManager.DEFAULT_MAX_NAME_COUNT +         //(# transactions @ DEFAULT_MAX_TRANSACION_NAME_COUNT no extra tag), Take note there is no Status/Method nor Error tag as those should not be added for SDK spans 
                     1, //(# other transactions no extra tag) 
                     measurements.size());
    }
    
    
}