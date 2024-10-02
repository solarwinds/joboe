package com.appoptics.api.ext;


import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import com.tracelytics.joboe.Constants;
import com.tracelytics.joboe.TokenBucketType;
import com.tracelytics.joboe.TraceDecision;
import com.tracelytics.joboe.TraceDecisionUtil;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.trace.Sampler;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.trace.*;
import io.opentelemetry.trace.spi.TraceProvider;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoService(TraceProvider.class)
public class AppOpticsAutoAgentTraceProvider implements TraceProvider {
    @Override
    public TracerProvider create() {
        return new TracerProvider() {
            @Override
            public Tracer get(String instrumentationName) {
                return getTracerProvider().get(instrumentationName);
            }

            @Override
            public Tracer get(String instrumentationName, String instrumentationVersion) {
                return getTracerProvider().get(instrumentationName, instrumentationVersion);
            }

            private TracerProvider getTracerProvider() {
                TracerSdkProvider tracerSdkProvider = TracerSdkProvider.builder().build();
                TraceConfig newConfig = TraceConfig.getDefault().toBuilder().setSampler(new Sampler() {
                    private Decision ALWAYS_ON_DECISION = new OtDecision(true);
                    private Decision ALWAYS_OFF_DECISION = new OtDecision(false);

                    @Override
                    public Decision shouldSample(@Nullable SpanContext parentContext, TraceId traceId, SpanId spanId, String name, Span.Kind spanKind, Map<String, AttributeValue> attributes, List<Link> parentLinks) {
                        TraceDecision aoTraceDecision = null;
                        if (!parentContext.isValid()) { //entry span of root
                            aoTraceDecision = TraceDecisionUtil.shouldTraceRequest(name, null, null, null); //TODO url can be extracted from attributes?
                        } else if (parentContext.isRemote()) { //entry span of this service - continuation of a distributed trace
                            String inXTraceID = buildInXTraceId(parentContext);
                            aoTraceDecision = TraceDecisionUtil.shouldTraceRequest(name, inXTraceID, null, null); //TODO url can be extracted from attributes?
                        }

                        Decision decision;
                        if (aoTraceDecision != null) {
                            return toOtDecision(aoTraceDecision);
                        } else {
                            if (parentContext.getTraceFlags().isSampled()) {
                                return ALWAYS_ON_DECISION;
                            }
                            if (parentLinks != null) {
                                // If any parent link is sampled keep the sampling decision.
                                for (Link parentLink : parentLinks) {
                                    if (parentLink.getContext().getTraceFlags().isSampled()) {
                                        return ALWAYS_ON_DECISION;
                                    }
                                }
                            }
                        }
                        return ALWAYS_OFF_DECISION;
                    }

                    private Decision toOtDecision(TraceDecision aoTraceDecision) {
                        Map<String, AttributeValue> attributes = new HashMap<String, AttributeValue>();
                        attributes.put("BucketCapacity", AttributeValue.doubleAttributeValue(aoTraceDecision.getTraceConfig().getBucketCapacity(TokenBucketType.REGULAR)));
                        attributes.put("BucketRate", AttributeValue.doubleAttributeValue(aoTraceDecision.getTraceConfig().getBucketRate(TokenBucketType.REGULAR)));
                        attributes.put("SampleRate", AttributeValue.longAttributeValue(aoTraceDecision.getTraceConfig().getSampleRate()));
                        attributes.put("SampleSource", AttributeValue.longAttributeValue(aoTraceDecision.getTraceConfig().getSampleRateSource().value()));
                        attributes.put("IsSampled", AttributeValue.booleanAttributeValue(aoTraceDecision.isSampled()));
                        attributes.put("IsBucketExhausted", AttributeValue.booleanAttributeValue(aoTraceDecision.isBucketExhausted()));
                        attributes.put("IsReportMetrics", AttributeValue.booleanAttributeValue(aoTraceDecision.isReportMetrics()));
                        return new OtDecision(aoTraceDecision.isSampled(), attributes);
                    }

                    @Override
                    public String getDescription() {
                        return "AppOptics Tracing Sampler";
                    }
                }).build();

                tracerSdkProvider.updateActiveTraceConfig(newConfig);
                return tracerSdkProvider;
            }
        };
    }

    private static String buildInXTraceId(SpanContext inContext) {
        String traceId = inContext.getTraceId().toLowerBase16();
        String spanId = inContext.getSpanId().toLowerBase16();
        boolean isSampled = inContext.getTraceFlags().isSampled();
        final String HEADER = "2B";
        String hexString = HEADER +
                Strings.padEnd(traceId, Constants.MAX_TASK_ID_LEN * 2, '0') +
                Strings.padEnd(spanId, Constants.MAX_OP_ID_LEN * 2, '0');
        hexString += isSampled ? "01" : "00";


        return hexString.toUpperCase();
    }

    private static final class OtDecision implements Sampler.Decision {

        private final boolean decision;
        private final Map<String, AttributeValue> attributes;

        OtDecision(boolean decision) {
            this(decision, Collections.EMPTY_MAP);
        }

        OtDecision(boolean decision, Map<String, AttributeValue> attributes) {
            this.decision = decision;
            this.attributes = Collections.unmodifiableMap(attributes);
        }

        @Override
        public boolean isSampled() {
            return decision;
        }

        @Override
        public Map<String, AttributeValue> attributes() {
            return Collections.emptyMap();
        }
    }
}
