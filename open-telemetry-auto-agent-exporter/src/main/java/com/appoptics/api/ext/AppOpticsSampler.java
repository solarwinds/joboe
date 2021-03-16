package com.appoptics.api.ext;

import com.google.auto.service.AutoService;
import com.tracelytics.joboe.TraceDecision;
import com.tracelytics.joboe.TraceDecisionUtil;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;

@AutoService(Sampler.class)
public class AppOpticsSampler implements Sampler {
    private SamplingResult PARENT_SAMPLED = SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE,
            Attributes.of(
                    AttributeKey.booleanKey("ao.detailedTracing"), true,
                    AttributeKey.booleanKey("ao.metrics"), true,
                    AttributeKey.booleanKey("ao.sampler"), true));
    private SamplingResult PARENT_NOT_SAMPLED = SamplingResult.create(SamplingDecision.DROP, Attributes.of(AttributeKey.booleanKey("ao.sampler"), true));
    private SamplingResult METRICS_ONLY = SamplingResult.create(SamplingDecision.RECORD_ONLY);
    private SamplingResult NOT_TRACED = SamplingResult.create(SamplingDecision.DROP);

    //public static TraceDecision shouldTraceRequest(String layer, String inXTraceID, XTraceOptions xTraceOptions, String resource) {
    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
        SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();

        if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
            String xTraceId = null;
            if (parentSpanContext.isRemote()) {
                xTraceId = Util.buildXTraceId(parentSpanContext);
            }
            TraceDecision aoTraceDecision = TraceDecisionUtil.shouldTraceRequest(name, xTraceId, null, null);
            return toOtSamplingResult(aoTraceDecision);
        } else { //follow parent's decision
            return parentSpanContext.isSampled() ? PARENT_SAMPLED : PARENT_NOT_SAMPLED; //todo metrics
        }
    }

    @Override
    public String getDescription() {
        return "AppOptics Sampler";
    }

    private SamplingResult toOtSamplingResult(TraceDecision aoTraceDecision) {
        if (aoTraceDecision.isSampled() || aoTraceDecision.isReportMetrics()) {
            SamplingDecision samplingDecision = SamplingDecision.RECORD_AND_SAMPLE;
            AttributesBuilder builder = Attributes.builder();
            builder.put("SampleRate", aoTraceDecision.getTraceConfig().getSampleRate());
            builder.put("SampleSource", aoTraceDecision.getTraceConfig().getSampleRateSourceValue());
            builder.put("BucketRate",aoTraceDecision.getTraceConfig().getBucketRate(aoTraceDecision.getRequestType().getBucketType()));
            builder.put("BucketCapacity",aoTraceDecision.getTraceConfig().getBucketCapacity(aoTraceDecision.getRequestType().getBucketType()));
            builder.put("RequestType", aoTraceDecision.getRequestType().name());
            builder.put("ao.detailedTracing", aoTraceDecision.isSampled());
            builder.put("ao.metrics", aoTraceDecision.isReportMetrics());
            builder.put("ao.sampler", true); //mark that it has been sampled by us
            Attributes attributes = builder.build();
            return SamplingResult.create(samplingDecision, attributes);
        } else {
            if (aoTraceDecision.isReportMetrics()) {
                return METRICS_ONLY; // is this correct? probably not...
            } else {
                return NOT_TRACED;
            }
        }
    }
}
