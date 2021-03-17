package com.appoptics.api.ext;

import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.RpcEventReporter;
import com.tracelytics.joboe.config.ProfilerSetting;
import com.tracelytics.joboe.rpc.RpcClientManager;
import com.tracelytics.profiler.Profiler;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class AppOpticsProfilingSpanProcessor implements SpanProcessor {
    static {
        Profiler.initialize(new ProfilerSetting(true, 20), RpcEventReporter.buildReporter(RpcClientManager.OperationType.PROFILING));
    }


    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
        if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
            Metadata metadata = Util.buildMetadata(span.getSpanContext());
            Profiler.addProfiledThread(Thread.currentThread(), metadata, metadata.getTraceId());
            span.setAttribute("ProfileSpans", 1);
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        SpanContext parentSpanContext = span.toSpanData().getParentSpanContext();
        if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
            Profiler.stopProfile(Util.buildTraceId(span.getSpanContext().getTraceIdBytes()));
        }
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }
}
