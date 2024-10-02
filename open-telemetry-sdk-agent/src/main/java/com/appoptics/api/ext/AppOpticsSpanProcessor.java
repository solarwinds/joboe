package com.appoptics.api.ext;

import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.ProfilerSetting;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.profiler.Profiler;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.trace.SpanId;

import java.util.WeakHashMap;

/**
 * AppOptics Span Processor that provides Profiling
 */
public class AppOpticsSpanProcessor implements SpanProcessor {
    private final boolean profilerEnabled;
    private final SpanProcessor processor;
    private final Tracer aoTracer = Tracer.INSTANCE;

    private AppOpticsSpanProcessor(SpanProcessor processor) {
        this.processor = processor;
        this.profilerEnabled = ConfigManager.getConfig(ConfigProperty.PROFILER) != null && ((ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER)).isEnabled();;
    }


    public static Builder newWrappedBuilder(SpanProcessor processor) {
        return new Builder(processor);
    }

    public static Builder newDefaultBuilder(String serviceKey) {
        return new Builder(SimpleSpansProcessor.newBuilder(new AppOpticsSdkSpanExporter(serviceKey)).build());
    }

    /** Builder class for {@link SimpleSpansProcessor}. */
    public static final class Builder {
        private final SpanProcessor spanProcessor;

        private Builder(SpanProcessor spanProcessor) {
            this.spanProcessor = spanProcessor;
        }

        public AppOpticsSpanProcessor build() {
            return new AppOpticsSpanProcessor(spanProcessor);
        }
    }

    private static final WeakHashMap<SpanId, Span> dummySpans = new WeakHashMap<SpanId, Span>();

    @Override
    public void onStart(ReadableSpan span) {
        if (processor.isStartRequired()) {
            processor.onStart(span);
        }
        if (isFlaggedForProfiling(span)) {
            Metadata dummySpanMetadata = AppOpticsContextUtils.buildAoMetadata(span.getSpanContext()); //creating this since profiler take a span, but this span is NOT translated to trace event later on, instead, it's the OT span that is translated into trace event
            Span dummySpan = aoTracer.buildSpan("dummy").withSpanProperty(Span.SpanProperty.ENTRY_SPAN_METADATA, dummySpanMetadata).start();
            Profiler.addProfiledThread(Thread.currentThread(), dummySpan);
            dummySpans.put(span.getSpanContext().getSpanId(), dummySpan);
        }
    }

    //TODO
    private boolean isFlaggedForProfiling(ReadableSpan span) {
        //return profilerEnabled;
        //TODO do not want to call SpanData(). But how can we pass info? span.getSpanContext().getTraceFlags() seems not suitable as it's global flag. we want a flag for the span only
        //Also this relies on our TraceConfig
        return span.toSpanData().getAttributes().getOrDefault("ao.isRoot", AttributeValue.booleanAttributeValue(false)).getBooleanValue();
    }

    @Override
    public boolean isStartRequired() {
        return profilerEnabled || processor.isStartRequired();
    }

    @Override
    public void onEnd(ReadableSpan span) {
        if (processor.isEndRequired()) {
            processor.onEnd(span);
        }

        if (isFlaggedForProfiling(span)) {
            Span dummySpan = dummySpans.get(span.getSpanContext().getSpanId());
            if (dummySpan != null) {
                Profiler.stopProfile(dummySpan);
                dummySpans.remove(span.getSpanContext().getSpanId());
            }
        }
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }

    @Override
    public void shutdown() {
        processor.shutdown();
    }

    @Override
    public void forceFlush() {
        processor.forceFlush();
    }
}
