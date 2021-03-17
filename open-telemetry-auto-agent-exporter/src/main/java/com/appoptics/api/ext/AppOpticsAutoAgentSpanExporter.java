package com.appoptics.api.ext;

import com.tracelytics.agent.Agent;
import com.tracelytics.joboe.*;
import com.tracelytics.joboe.config.InvalidConfigException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Span exporter to be used with the OpenTelemetry auto agent
 */
public class AppOpticsAutoAgentSpanExporter implements SpanExporter {
    private static final AttributeKey<Boolean> AO_SAMPLER_KEY = AttributeKey.booleanKey("ao.sampler");

    private AppOpticsAutoAgentSpanExporter(String serviceKey) {

    }

    static Builder newBuilder(String serviceKey) {
        return new Builder(serviceKey);
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> collection) {
        for (SpanData spanData : collection) {
            if (spanData.hasEnded() && Boolean.TRUE == spanData.getAttributes().get(AO_SAMPLER_KEY)) {
                try {
                    Metadata parentMetadata = null;
                    if (spanData.getParentSpanContext().isValid()) {
                        parentMetadata = Util.buildMetadata(spanData.getParentSpanContext());
                    }

                    String entryXTraceId = Util.buildXTraceId(spanData.getSpanContext());

                    String spanName = spanData.getKind().toString() + "." + spanData.getName();

                    Metadata spanMetadata = new Metadata(entryXTraceId);
                    spanMetadata.randomizeOpID(); //get around the metadata logic, this op id is not used
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
                    entryEvent.report(spanMetadata);

                    entryEvent.addInfo(getTags(spanData.getAttributes()));


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
        }

        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }




    private static final Map<String, String> ATTRIBUTE_TO_TAG = new HashMap<String, String>();
    private static final Map<String, TypeConverter<?>> TAG_VALUE_TYPE = new HashMap<String, TypeConverter<?>>();
    static {
        ATTRIBUTE_TO_TAG.put("http.status_code", "Status");
        ATTRIBUTE_TO_TAG.put("net.peer.ip", "ClientIP");
        ATTRIBUTE_TO_TAG.put("http.url", "URL");
        ATTRIBUTE_TO_TAG.put("http.method", "HTTPMethod");
        ATTRIBUTE_TO_TAG.put("db.connection_string", "RemoteHost");
        ATTRIBUTE_TO_TAG.put("db.system", "Flavor");
        ATTRIBUTE_TO_TAG.put("db.statement", "Query");
        ATTRIBUTE_TO_TAG.put("db.url", "RemoteHost");
        TAG_VALUE_TYPE.put("Status", IntConverter.INSTANCE);
    }

    private Map<String,?> getTags(Attributes attributes) {
        Map<String, Object> tags = new HashMap<String, Object>();
        for (Map.Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
            Object attributeValue = entry.getValue();
            String attributeKey = entry.getKey().getKey();
            tags.put(attributeKey, attributeValue);

            if (ATTRIBUTE_TO_TAG.containsKey(attributeKey)) {
                String tagKey = ATTRIBUTE_TO_TAG.get(attributeKey);
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

//    private static Object getAttributeValue(AttributeValue attributeValue)   {
//        switch (attributeValue.getType()) {
//            case BOOLEAN:
//                return attributeValue.getBooleanValue();
//            case LONG:
//                return attributeValue.getLongValue();
//            case DOUBLE:
//                return attributeValue.getDoubleValue();
//            case STRING:
//                return attributeValue.getStringValue();
//            default:
//                System.err.println("Unknown type " + attributeValue.getType());
//        }
//        return null;
//    }



    @Override
    public void close() {

    }

    public static class Builder {

        private final String serviceKey;

        public Builder(String serviceKey) {
            this.serviceKey = serviceKey;
        }

        AppOpticsAutoAgentSpanExporter build() {
            return new AppOpticsAutoAgentSpanExporter(serviceKey);
        }
    }
}
