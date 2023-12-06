package com.solarwinds.joboe.span.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.solarwinds.joboe.span.impl.Span.SpanProperty;
import com.solarwinds.joboe.span.impl.Span.TraceProperty;
import com.solarwinds.util.HttpUtils;

public class InboundMetricMeasurementSpanReporter extends MetricMeasurementSpanReporter {
    public static final String TRANSACTION_LATENCY_METRIC_NAME = "TransactionResponseTime";
    
    public static MetricMeasurementSpanReporter REPORTER = new InboundMetricMeasurementSpanReporter();
    
    private InboundMetricMeasurementSpanReporter() {
        super(TRANSACTION_LATENCY_METRIC_NAME);
    }
    
    @Override
    public void reportMetrics(Span span, long duration) {
        if (!span.isRoot()) { //do not report inbound metrics if this is not the root span
            return;
        }
        
        String transactionName = span.getTracePropertyValue(TraceProperty.TRANSACTION_NAME);
        
        Map<String, String> primaryKeys = Collections.singletonMap("TransactionName", transactionName);
        boolean hasError = span.getTracePropertyValue(TraceProperty.HAS_ERROR);
        
        Map<String, String> optionalKeys = new HashMap<String, String>();
        
        if (!span.getSpanPropertyValue(SpanProperty.IS_SDK)) { //only add these tags if its OOTB instrumentation
            Integer status = (Integer) span.getTags().get("Status");
            //special handling for status code
            if (!hasError && status != null) {
                hasError = HttpUtils.isServerErrorStatusCode(status); //do not attempt to override the property if it's already explicitly set
            }
    
            if (status != null) {
                optionalKeys.put("HttpStatus", String.valueOf(status));
            }
        
            String method = (String) span.getTags().get("HTTPMethod");
            if (method != null) {
                optionalKeys.put("HttpMethod", method);
            }
        }
        
        if (hasError) {
            optionalKeys.put("Errors", "true");
        }
        
//        optionalKeys.putAll(span.getSpanPropertyValue(SpanProperty.METRIC_TAGS));
        
        
        recordMeasurementEntry(primaryKeys, optionalKeys, duration);
    }
}