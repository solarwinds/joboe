package com.appoptics.api.ext;

import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.span.impl.ScopeManager;
import io.grpc.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.trace.*;

import java.util.WeakHashMap;
import java.util.logging.Logger;

/**
 * A workaround class to synchronize OT scope with AO scope, we do NOT need this if we can detect scope via some interpreter mechanism as proposed in
 * https://github.com/open-telemetry/opentelemetry-java/issues/922
 */
public class AppOpticsTracerProvider implements TracerProvider {
    private static Logger logger = Logger.getLogger(AppOpticsTracerProvider.class.getName());
    final TracerSdkProvider tracerSdkProvider = TracerSdkProvider.builder().build();




    @Override
    public Tracer get(String instrumentationName) {
        final Tracer sdkTracer = tracerSdkProvider.get(instrumentationName);
        return new ContextChangeNotifyingTracer(sdkTracer, AppOpticsContextChangeListener.INSTANCE);
    }

    @Override
    public Tracer get(String instrumentationName, String instrumentationVersion) {
        final Tracer sdkTracer = tracerSdkProvider.get(instrumentationName, instrumentationVersion);
        return new ContextChangeNotifyingTracer(sdkTracer, AppOpticsContextChangeListener.INSTANCE);
    }

    /**
     * Updates the active {@link TraceConfig}.
     *
     * @param traceConfig the new active {@code TraceConfig}.
     */
    public void updateActiveTraceConfig(TraceConfig traceConfig) {
        tracerSdkProvider.updateActiveTraceConfig(traceConfig);
    }

    /**
     * Adds a new {@code SpanProcessor} to this {@code Tracer}.
     *
     * <p>Any registered processor cause overhead, consider to use an async/batch processor especially
     * for span exporting, and export to multiple backends using the {@link
     * io.opentelemetry.sdk.trace.export.MultiSpanExporter}.
     *
     * @param spanProcessor the new {@code SpanProcessor} to be added.
     */
    public void addSpanProcessor(SpanProcessor spanProcessor) {
        tracerSdkProvider.addSpanProcessor(spanProcessor);
    }


    private static class ContextChangeNotifyingTracer implements Tracer {
        private final Tracer sdkTracer;
        private final ContextChangeListener listener;

        private ContextChangeNotifyingTracer(Tracer sdkTracer, ContextChangeListener listener) {
            this.sdkTracer = sdkTracer;
            this.listener = listener;
        }

        @Override
        public Span getCurrentSpan() {
            return sdkTracer.getCurrentSpan();
        }

        @Override
        public Scope withSpan(Span span) {
            Context oldContext = Context.current();
            final Scope sdkScope = sdkTracer.withSpan(span);
            Scope wrappedScope = new WrappedScope(sdkScope, listener);

            if (listener != null) {
                listener.onContextChange(oldContext, Context.current());
            }
            return wrappedScope;
        }

        @Override
        public Span.Builder spanBuilder(String spanName) {
            return sdkTracer.spanBuilder(spanName);
        }
    }
}
