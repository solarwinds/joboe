package com.appoptics.api.ext;

import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.ProfilerSetting;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.profiler.Profiler;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.trace.SpanId;

import java.util.WeakHashMap;

public class AppOpticsProfilingSpanProcessor implements SpanProcessor {
    private static boolean PROFILER_ENABLED = ConfigManager.getConfig(ConfigProperty.PROFILER) != null && ((ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER)).isEnabled();;
    private final io.opentelemetry.sdk.trace.SpanProcessor processor;
    private final Tracer aoTracer = Tracer.INSTANCE;

    public AppOpticsProfilingSpanProcessor(io.opentelemetry.sdk.trace.SpanProcessor processor) {
        this.processor = processor;
    }

    private static final WeakHashMap<SpanId, Span> dummySpans = new WeakHashMap<SpanId, Span>();

    @Override
    public void onStart(ReadableSpan span) {
        if (processor.isStartRequired()) {
            processor.onStart(span);
        }
        if (isFlaggedForProfiling(span)) {
            try {
                Metadata dummySpanMetadata = new Metadata(Util.buildXTraceId(span.getSpanContext()));
                Span dummySpan = aoTracer.buildSpan("dummy").withSpanProperty(Span.SpanProperty.ENTRY_SPAN_METADATA, dummySpanMetadata).start();
                Profiler.addProfiledThread(Thread.currentThread(), dummySpan);
                dummySpans.put(span.getSpanContext().getSpanId(), dummySpan);
            } catch (OboeException e) {
                e.printStackTrace();
            }
        }
    }

    //TODO
    private boolean isFlaggedForProfiling(ReadableSpan span) {
        return span.toSpanData().getAttributes().getOrDefault("ao.profile", AttributeValue.booleanAttributeValue(false)).getBooleanValue();
    }

    @Override
    public boolean isStartRequired() {
        return PROFILER_ENABLED || processor.isStartRequired();
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
            }
        }
        //cannot remove dummySpan here as spans might not have been exported yet
    }

    @Override
    public boolean isEndRequired() {
        return PROFILER_ENABLED || processor.isStartRequired();
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
