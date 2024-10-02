package com.appoptics.api.ext;

import com.tracelytics.instrumentation.HeaderConstants;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.*;
import io.opentelemetry.trace.propagation.HttpTraceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import static io.opentelemetry.internal.Utils.checkNotNull;

/**
 * Has to call this if running our agent along side with the OT SDK
 *
 * Mostly initialize hooks for context propagation/synchronization
 *
 * Other settings (exporter, span processor) should be done outside of this via the regular OT SDK way
 */
public class AppOpticsAgentSdk {
    public static void init() {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        final Tracer otTracer = OpenTelemetry.getTracerProvider().get("");

        // Use the OpenTelemetry SDK
//        TracerSdkProvider tracerProvider = TracerSdkProvider.builder().build();
//
//        tracerProvider.updateActiveTraceConfig(AppOpticsTraceConfig.getTraceConfig()); //use our own sampler
//        // Set to process the spans by the log exporter
//        //tracerProvider.addSpanProcessor(SimpleSpansProcessor.newBuilder(traceEventExporter).build());
//        tracerProvider.addSpanProcessor(new AppOpticsSpanProcessor());

        //TODO need some better way to modify the tracestate to have our x-trace id : https://github.com/open-telemetry/opentelemetry-java/issues/278#issuecomment-613632006
        OpenTelemetry.setPropagators(buildPropagators(OpenTelemetry.getPropagators()));

        //AO -> OT scope sync
        //Add hook to AO javaagent to create OT scope when root span is created
        ScopeManager.registerListener(new ScopeManager.ScopeListener() {
            private final WeakHashMap<Scope, io.opentelemetry.context.Scope> lookup = new WeakHashMap<Scope, io.opentelemetry.context.Scope>();
            @Override
            public void onAddScope(Scope agentScope) {
                //ContextUtils.withScopedContext(Context.current().withValue()
                Metadata agentContext = agentScope.span().context().getMetadata();
                SpanContext otSpanContext = SpanContext.create(TraceId.fromLowerBase16(agentContext.taskHexString().toLowerCase(), 0),
                        SpanId.fromLowerBase16(agentContext.opHexString().toLowerCase(), 0),
                        TraceFlags.builder().setIsSampled(agentContext.isSampled()).build(),
                        TraceState.getDefault());

                //no recursive problem here as TracingContextUtils does not notify on OT Span creation
                io.opentelemetry.context.Scope otScope = TracingContextUtils.currentContextWith(DefaultSpan.create(otSpanContext));

                lookup.put(agentScope, otScope);
            }

            @Override
            public void onRemoveScope(Scope agentScope) {
                io.opentelemetry.context.Scope otScope = lookup.remove(agentScope);
                if (otScope != null) {
                    otScope.close();
                }
            }
        });

        //TODO the other way around : how do we detect Scope has been created and set to AO context?
        //need https://github.com/open-telemetry/opentelemetry-java/pull/923
    }

    private static ContextPropagators buildPropagators(final ContextPropagators propagators) {
        return new ContextPropagators() {
            @Override
            public HttpTextFormat getHttpTextFormat() {
                return new HttpTraceContext() {
                    static final String TRACE_PARENT = "traceparent";
                    static final String TRACE_STATE = "tracestate";
                    private final String VERSION = "00";
                    private final int VERSION_SIZE = 2;
                    private final char TRACEPARENT_DELIMITER = '-';
                    private final int TRACEPARENT_DELIMITER_SIZE = 1;
                    private final int TRACE_ID_HEX_SIZE = 2 * TraceId.getSize();
                    private final int SPAN_ID_HEX_SIZE = 2 * SpanId.getSize();
                    private final int TRACE_OPTION_HEX_SIZE = 2 * TraceFlags.getSize();
                    private final int TRACE_ID_OFFSET = VERSION_SIZE + TRACEPARENT_DELIMITER_SIZE;
                    private final int SPAN_ID_OFFSET =
                            TRACE_ID_OFFSET + TRACE_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
                    private final int TRACE_OPTION_OFFSET =
                            SPAN_ID_OFFSET + SPAN_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
                    private final int TRACEPARENT_HEADER_SIZE = TRACE_OPTION_OFFSET + TRACE_OPTION_HEX_SIZE;
                    private final int TRACESTATE_MAX_SIZE = 512;
                    private final char TRACESTATE_KEY_VALUE_DELIMITER = '=';
                    private final char TRACESTATE_ENTRY_DELIMITER = ',';

                    @Override
                    public <C> void inject(Context context, C carrier, Setter<C> setter) {
                        checkNotNull(context, "context");
                        checkNotNull(setter, "setter");
                        checkNotNull(carrier, "carrier");

                        Span span = TracingContextUtils.getSpanWithoutDefault(context);
                        if (span == null) {
                            return;
                        }

                        injectImpl(span.getContext(), carrier, setter);
                    }

                    private <C> void injectImpl(SpanContext spanContext, C carrier, Setter<C> setter) {
                        char[] chars = new char[TRACEPARENT_HEADER_SIZE];
                        chars[0] = VERSION.charAt(0);
                        chars[1] = VERSION.charAt(1);
                        chars[2] = TRACEPARENT_DELIMITER;
                        spanContext.getTraceId().copyLowerBase16To(chars, TRACE_ID_OFFSET);
                        chars[SPAN_ID_OFFSET - 1] = TRACEPARENT_DELIMITER;
                        spanContext.getSpanId().copyLowerBase16To(chars, SPAN_ID_OFFSET);
                        chars[TRACE_OPTION_OFFSET - 1] = TRACEPARENT_DELIMITER;
                        spanContext.getTraceFlags().copyLowerBase16To(chars, TRACE_OPTION_OFFSET);
                        setter.set(carrier, TRACE_PARENT, new String(chars));
                        List<TraceState.Entry> entries = spanContext.getTraceState().getEntries();
                        if (com.tracelytics.joboe.Context.getMetadata().isValid()) { //add our x-trace id to tracestate here
                            entries = new ArrayList<TraceState.Entry>(spanContext.getTraceState().getEntries());
                            entries.add(TraceState.Entry.create(HeaderConstants.XTRACE_HEADER.toLowerCase(), com.tracelytics.joboe.Context.getMetadata().toHexString()));
                        }

                        if (entries.isEmpty()) {
                            // No need to add an empty "tracestate" header.
                            return;
                        }
                        StringBuilder stringBuilder = new StringBuilder(TRACESTATE_MAX_SIZE);
                        for (TraceState.Entry entry : entries) {
                            if (stringBuilder.length() != 0) {
                                stringBuilder.append(TRACESTATE_ENTRY_DELIMITER);
                            }
                            stringBuilder
                                    .append(entry.getKey())
                                    .append(TRACESTATE_KEY_VALUE_DELIMITER)
                                    .append(entry.getValue());
                        }
                        setter.set(carrier, TRACE_STATE, stringBuilder.toString());
                    }
                };
            }
        };
    }

//    public static final class AppOpticsTracerProvider implements TracerProvider {
//
//        @Override
//        public Tracer get(String instrumentationName) {
//
//        }
//
//        @Override
//        public Tracer get(String instrumentationName, String instrumentationVersion) {
//            return null;
//        }
//    }
//
//    public static final class AppOpticsTracer implements Tracer {
//        private final Tracer sdkTracer;
//
//        private AppOpticsTracer(Tracer sdkTracer) {
//            this.sdkTracer = sdkTracer;
//        }
//
//        @Override
//        public Span getCurrentSpan() {
//            return sdkTracer.getCurrentSpan();
//        }
//
//        @Override
//        public Scope withSpan(Span span) {
//            return sdkTracer.withSpan(span);
//        }
//
//        @Override
//        public Span.Builder spanBuilder(String spanName) {
//            final Span.Builder sdkBuilder = sdkTracer.spanBuilder(spanName);
//            return new Span.Builder() {
//                @Override
//                public Span.Builder setParent(Span parent) {
//                    return sdkBuilder.setParent(parent);
//                }
//
//                @Override
//                public Span.Builder setParent(SpanContext remoteParent) {
//                    return sdkBuilder.setParent(remoteParent);
//                }
//
//                @Override
//                public Span.Builder setNoParent() {
//                    return sdkBuilder.setNoParent();
//                }
//
//                @Override
//                public Span.Builder addLink(SpanContext spanContext) {
//                    return sdkBuilder.addLink(spanContext);
//                }
//
//                @Override
//                public Span.Builder addLink(SpanContext spanContext, Map<String, AttributeValue> attributes) {
//                    return sdkBuilder.addLink(spanContext, attributes);
//                }
//
//                @Override
//                public Span.Builder addLink(Link link) {
//                    return sdkBuilder.addLink(link)
//                }
//
//                @Override
//                public Span.Builder setAttribute(String key, @Nullable String value) {
//                    return sdkBuilder.setAttribute(key, value);
//                }
//
//                @Override
//                public Span.Builder setAttribute(String key, long value) {
//                    return sdkBuilder.setAttribute(key, value);
//                }
//
//                @Override
//                public Span.Builder setAttribute(String key, double value) {
//                    return sdkBuilder.setAttribute(key, value);
//                }
//
//                @Override
//                public Span.Builder setAttribute(String key, boolean value) {
//                    return sdkBuilder.setAttribute(key, value);
//                }
//
//                @Override
//                public Span.Builder setAttribute(String key, AttributeValue value) {
//                    return sdkBuilder.setAttribute(key, value);
//                }
//
//                @Override
//                public Span.Builder setSpanKind(Span.Kind spanKind) {
//                    return sdkBuilder.setSpanKind(spanKind);
//                }
//
//                @Override
//                public Span.Builder setStartTimestamp(long startTimestamp) {
//                    return sdkBuilder.setStartTimestamp(startTimestamp);
//                }
//
//                @Override
//                public Span startSpan() {
//                    return skdBuilder
//                }
//            }
//        }
//    }

//    public static final class AppOpticsSpanProcessor implements io.opentelemetry.sdk.trace.SpanProcessor {
//        private final io.opentelemetry.sdk.trace.SpanProcessor wrapped;
//        private final SpanExporter exporter = new AppOpticsSdkSpanExporter();
//
//        public AppOpticsSpanProcessor() {
//            this(null);
//        }
//
//        public AppOpticsSpanProcessor(io.opentelemetry.sdk.trace.SpanProcessor wrapped) {
//            this.wrapped = wrapped;
//        }
//
//        @Override
//        public void onStart(ReadableSpan span) {
//            if (wrapped != null && this.wrapped.isStartRequired()) {
//                wrapped.onStart(span);
//            }
//
//            exporter.export(Collections.singletonList(span.toSpanData()));
//        }
//
//        @Override
//        public boolean isStartRequired() {
//            return true;
//        }
//
//        @Override
//        public void onEnd(ReadableSpan span) {
//            if (wrapped != null && this.wrapped.isEndRequired()) {
//                wrapped.onEnd(span);
//            }
//            exporter.export(Collections.singletonList(span.toSpanData()));
//        }
//
//        @Override
//        public boolean isEndRequired() {
//            return true;
//        }
//
//        @Override
//        public void shutdown() {
//            if (wrapped != null) {
//                wrapped.shutdown();
//            }
//        }
//
//        @Override
//        public void forceFlush() {
//            if (wrapped != null) {
//                wrapped.forceFlush();
//            }
//        }
//    }
}
