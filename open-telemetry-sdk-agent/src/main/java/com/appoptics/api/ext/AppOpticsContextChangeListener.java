package com.appoptics.api.ext;

import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.Span;
import io.grpc.Context;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.TracingContextUtils;

import java.util.WeakHashMap;
import java.util.logging.Logger;


/*
   OT scope -> AO scope sync
   if AO has an active scope, do NOT update the task ID, else update the task ID by padding zeros to the OT trace ID.
   Then set OT span id as AO op ID
   */
public class AppOpticsContextChangeListener implements ContextChangeListener {
    public static final AppOpticsContextChangeListener INSTANCE = new AppOpticsContextChangeListener();
    private WeakHashMap<SpanContext, Scope> contextLookup = new WeakHashMap<SpanContext, com.tracelytics.joboe.span.impl.Scope>();
    private static Logger logger = Logger.getLogger(AppOpticsContextChangeListener.class.getName());

    final com.tracelytics.joboe.span.impl.Tracer aoTracer = com.tracelytics.joboe.span.impl.Tracer.INSTANCE;

    private AppOpticsContextChangeListener() {

    }

    @Override
    public void onContextChange(Context oldContext, Context newContext) {
        SpanContext oldSpanContext = TracingContextUtils.getSpan(oldContext).getContext();
        SpanContext newSpanContext= TracingContextUtils.getSpan(newContext).getContext();

        if (newSpanContext.isValid()) {
            if (!contextLookup.containsKey(newSpanContext)) { //adding a new scope
                com.tracelytics.joboe.span.impl.Scope aoScope = createNewAoScope(newSpanContext);
                contextLookup.put(newSpanContext, aoScope);
            } else { //reverting back to a new context (parent), that means old context (nested) is closing
                if (oldSpanContext.isValid()) {
                    closeAoScope(oldSpanContext);
                } else {
                    logger.warning("Unexpected context status - reverting to parent context " + newSpanContext + ", but old context (nested) is not valid");
                }
            }
        } else {
            if (oldSpanContext.isValid()) { //last context scope closing, no more context left
                closeAoScope(oldSpanContext);
            } else {
                logger.warning("Unexpected context status - both new and old context are invalid");
            }
        }
    }

    private void closeAoScope(SpanContext context) {
        com.tracelytics.joboe.span.impl.Scope aoScope = contextLookup.get(context);
        if (aoScope != null) {
            aoScope.close();
        } else {
            logger.warning("Unexpected context status - reverting to parent context from child context " + context + " but cannot find a corresponding ao scope of child context to close");
        }
    }

    private com.tracelytics.joboe.span.impl.Scope createNewAoScope(SpanContext otSpanContext) {
        if (otSpanContext.isValid()) {
            Metadata aoMetadata = AppOpticsContextUtils.buildAoMetadata(otSpanContext);
            com.tracelytics.joboe.span.impl.Span dummySpan = aoTracer.buildSpan("dummy")
                    .withSpanProperty(com.tracelytics.joboe.span.impl.Span.SpanProperty.ENTRY_SPAN_METADATA, aoMetadata)
                    .withSpanProperty(Span.SpanProperty.IS_SYNC_SPAN, true) //such that the creation of this would not recursively trigger AO -> OT sync
                    .start();
            return aoTracer.activateSpan(dummySpan);
        } else {
            logger.warning("Cannot sync context from OT -> AO, the OT Context do not have valid SpanContext: " + otSpanContext);
            return null;
        }
    }

}
