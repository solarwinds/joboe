package com.appoptics.api.ext;

import com.tracelytics.ext.google.common.base.Strings;
import com.tracelytics.instrumentation.HeaderConstants;
import com.tracelytics.joboe.*;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.trace.Sampler;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.trace.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AppOpticsTraceConfig {
    private static final Sampler.Decision ALWAYS_ON_DECISION = new OtDecision(true);
    private static final Sampler.Decision ALWAYS_OFF_DECISION = new OtDecision(false);
    private static final WeakHashMap<TraceId, TraceDecision> traceDecisions = new WeakHashMap<TraceId, TraceDecision>();

    static {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
    }

    public static TraceConfig getTraceConfig() {
        TraceConfig newConfig = TraceConfig.getDefault().toBuilder().setSampler(new Sampler() {
            @Override
            public Decision shouldSample(@Nullable SpanContext parentContext, TraceId traceId, SpanId spanId, String name, Span.Kind spanKind, Map<String, AttributeValue> attributes, List<Link> parentLinks) {

                //String xtraceHeader = spanData.getTraceState().get(HeaderConstants.XTRACE_HEADER.toLowerCase());

                TraceDecision aoServiceEntryDecision = null;
                if (parentContext == null || !parentContext.isValid()) { //entry span of root
                    if (Context.getMetadata().isValid()) { //already has trace context from our other instrumentation, use that decision
                        return Context.getMetadata().isSampled() ? ALWAYS_ON_DECISION : ALWAYS_OFF_DECISION;
                    }
                    aoServiceEntryDecision = TraceDecisionUtil.shouldTraceRequest(name, null, null, null); //TODO url can be extracted from attributes?
                } else if (parentContext.isRemote()) { //entry span of this service - continuation of a distributed trace
                    //String inXTraceID = buildInXTraceId(parentContext);
                    String inXTraceID = parentContext.getTraceState().get(HeaderConstants.XTRACE_HEADER.toLowerCase());
                    aoServiceEntryDecision = TraceDecisionUtil.shouldTraceRequest(name, inXTraceID, null, null); //TODO url can be extracted from attributes?
                }


                if (aoServiceEntryDecision != null) {
                    traceDecisions.put(traceId, aoServiceEntryDecision); //for the span processor later on. Not ideal to cache it like this. We should probably factory out the sampling logic away from the builder?
                    Decision otDecision = toOtDecision(aoServiceEntryDecision);

                    return otDecision;
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
                if (aoTraceDecision.getTraceConfig() != null) {
                    attributes.put("BucketCapacity", AttributeValue.doubleAttributeValue(aoTraceDecision.getTraceConfig().getBucketCapacity(TokenBucketType.REGULAR)));
                    attributes.put("BucketRate", AttributeValue.doubleAttributeValue(aoTraceDecision.getTraceConfig().getBucketRate(TokenBucketType.REGULAR)));
                    attributes.put("SampleRate", AttributeValue.longAttributeValue(aoTraceDecision.getTraceConfig().getSampleRate()));
                    attributes.put("SampleSource", AttributeValue.longAttributeValue(aoTraceDecision.getTraceConfig().getSampleRateSource().value()));
                }
                attributes.put("IsSampled", AttributeValue.booleanAttributeValue(aoTraceDecision.isSampled()));
                attributes.put("IsBucketExhausted", AttributeValue.booleanAttributeValue(aoTraceDecision.isBucketExhausted()));
                attributes.put("IsReportMetrics", AttributeValue.booleanAttributeValue(aoTraceDecision.isReportMetrics()));
                attributes.put("ao.isRoot", AttributeValue.booleanAttributeValue(true));
                return new OtDecision(aoTraceDecision.isSampled(), attributes);
            }

            @Override
            public String getDescription() {
                return "AppOptics Tracing Sampler";
            }
        }).build();
        return newConfig;
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

    static TraceDecision getTraceDecision(TraceId traceId) {
        return traceDecisions.get(traceId);
    }

    static void clearTraceDecision(TraceId traceId) {
        traceDecisions.remove(traceId);
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
            return attributes;
        }
    }
}
