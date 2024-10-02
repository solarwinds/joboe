package com.appoptics.api.ext;

import com.tracelytics.agent.Agent;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.EventImpl;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.span.impl.*;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AppOpticsSdkSpanExporter implements SpanExporter {

    private final Tracer aoTracer;

    private final SpanReporter[] metricSpanReporters;
    public AppOpticsSdkSpanExporter(String serviceKey) {
        if (Agent.getStatus() == Agent.AgentStatus.UNINITIALIZED) { //as a standalone
            try {
                Agent.initConfig(null, serviceKey);
            } catch (InvalidConfigException e) {
                e.printStackTrace();
            }
            AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        }

        aoTracer = Tracer.INSTANCE;
        metricSpanReporters = new SpanReporter[] { MetricHistogramSpanReporter.REPORTER, InboundMetricMeasurementSpanReporter.REPORTER };

    }

    @Override
    public ResultCode export(Collection<SpanData> collection) {
        for (SpanData spanData : collection) {
            if (spanData.getHasEnded()) {
                exportTraceEvent(spanData);
                exportInboundMetrics(spanData);
            }
        }
        return ResultCode.SUCCESS;
    }

    private void exportTraceEvent(SpanData spanData) {
        try {
            Metadata parentMetadata;
            if (spanData.getParentSpanId().isValid()) {
                parentMetadata = AppOpticsContextUtils.buildAoMetadata(spanData.getTraceState(), spanData.getTraceId(), spanData.getParentSpanId(), spanData.getTraceFlags());
            } else {
                parentMetadata = null;
            }

            String spanName = spanData.getKind().toString() + "." + spanData.getName();

            Metadata spanMetadata = AppOpticsContextUtils.buildAoMetadata(spanData.getTraceState(), spanData.getTraceId(), spanData.getSpanId(), spanData.getTraceFlags());
            String entryXTraceId = spanMetadata.toHexString();
            spanMetadata.randomizeOpID(); //get around the metadata logic, this op id is not used, only for comparison when entry event is reported

            Event entryEvent;

            if (parentMetadata != null) {
                entryEvent = new EventImpl(parentMetadata, entryXTraceId, true);
            } else {
                entryEvent = new EventImpl(null, entryXTraceId, false);
            }

            entryEvent.addInfo(
                    "Label", "entry",
                    "Layer", spanName);
            entryEvent.setTimestamp(spanData.getStartEpochNanos() / 1000);
            entryEvent.addInfo(getTags(spanData.getAttributes()));
            entryEvent.report(spanMetadata);

            Event exitEvent = new EventImpl(spanMetadata,true); //exit ID has to be generated
            exitEvent.addInfo(
                    "Label", "exit",
                    "Layer", spanName);
            exitEvent.setTimestamp(spanData.getEndEpochNanos() / 1000);
            exitEvent.report(spanMetadata);
        } catch (OboeException e) {
            e.printStackTrace();
        }
    }

    private void exportInboundMetrics(SpanData spanData) {
        if (spanData.getAttributes().getOrDefault("ao.isRoot", AttributeValue.booleanAttributeValue(false)).getBooleanValue()) {
            //easier to just convert it to span and use existing Span reporter for now
            Tracer.SpanBuilder builder = aoTracer.buildSpan(spanData.getName()).withStartTimestamp(spanData.getStartEpochNanos() / 1000);
            builder = builder.withReporters(metricSpanReporters);
            builder.withTags(getTags(spanData.getAttributes()));
            Span metricSpan = builder.start();

            //then trigger finish right the way
            metricSpan.finish(spanData.getEndEpochNanos() / 1000);
        }
    }

    private static final Map<String, String> ATTRIBUTE_TO_TAG = new HashMap<String, String>();
    private static final Map<String, TypeConverter<?>> TAG_VALUE_TYPE = new HashMap<String, TypeConverter<?>>();
    static {
        ATTRIBUTE_TO_TAG.put("http.status_code", "Status");
        ATTRIBUTE_TO_TAG.put("net.peer.ip", "ClientIP");
        ATTRIBUTE_TO_TAG.put("http.url", "URL");
        ATTRIBUTE_TO_TAG.put("http.method", "Method");
        ATTRIBUTE_TO_TAG.put("db.statement", "Query");
        ATTRIBUTE_TO_TAG.put("db.url", "RemoteHost");


        TAG_VALUE_TYPE.put("Status", IntConverter.INSTANCE);
    }

    private Map<String,?> getTags(Map<String, AttributeValue> attributes) {
        Map<String, Object> tags = new HashMap<String, Object>();
        for (Map.Entry<String, AttributeValue> entry : attributes.entrySet()) {
            Object attributeValue = getAttributeValue(entry.getValue());
            tags.put(entry.getKey(), attributeValue);

            if (ATTRIBUTE_TO_TAG.containsKey(entry.getKey())) {
                String tagKey = ATTRIBUTE_TO_TAG.get(entry.getKey());
                if (TAG_VALUE_TYPE.containsKey(tagKey)) {
                    attributeValue = TAG_VALUE_TYPE.get(tagKey).convert(attributeValue);
                }
                tags.put(tagKey, attributeValue);
            }

        }
        return tags;
    }

    interface TypeConverter<T> {
        T convert(Object rawValue);
    }

    private static class IntConverter implements TypeConverter<Integer> {
        static final IntConverter INSTANCE = new IntConverter();
        @Override
        public Integer convert(Object rawValue) {
            if (rawValue instanceof Number) {
                return ((Number) rawValue).intValue();
            } else if (rawValue instanceof String) {
                return Integer.valueOf((String) rawValue);
            } else {
                return null;
            }
        }
    }

    private static Object getAttributeValue(AttributeValue attributeValue)   {
        switch (attributeValue.getType()) {
            case BOOLEAN:
                return attributeValue.getBooleanValue();
            case LONG:
                return attributeValue.getLongValue();
            case DOUBLE:
                return attributeValue.getDoubleValue();
            case STRING:
                return attributeValue.getStringValue();
            default:
                System.err.println("Unknown type " + attributeValue.getType());
        }
        return null;
    }

    @Override
    public void shutdown() {

    }
}
