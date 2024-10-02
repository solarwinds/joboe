package com.appoptics.api.ext.impl;

import java.util.Map;
import java.util.Map.Entry;

import com.appoptics.api.ext.TraceEvent;
import com.appoptics.api.ext.model.EventWrapper;
import com.appoptics.api.ext.model.NoOpEvent;
import com.appoptics.api.ext.model.StartTraceEvent;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

public class TraceHandler implements ITraceHandler {
    private final Logger logger = LoggerFactory.getLogger();
    /**
     * {@inheritDoc}
     */
    public TraceEvent startTrace(String layer) {
        return startOrContinueTrace(layer, null);
    }
    
    /**
     * {@inheritDoc}
     */
    public TraceEvent continueTrace(String layer, String inXTraceID) {
        return startOrContinueTrace(layer, inXTraceID);
    }

    /**
     * {@inheritDoc}
     */
    public String endTrace(String layer) {
        return endTrace(layer, (Map<String, Object>) null);
    }


    /**
     * {@inheritDoc}
     */
    public String endTrace(String layer, Map<String, Object> info) {
        Scope currentScope = ScopeManager.INSTANCE.active();
        if (currentScope == null) {
            logger.warn("Attempt to end a Trace but there's no active scope. Ignoring the operation");
            return "";
        }
        Span currentSpan = currentScope.span();
        
        if (!currentSpan.getSpanPropertyValue(SpanProperty.IS_SDK)) {
            logger.warn("Attempt to end a Trace but the active span was not created by SDK call. Ignoring the operation");
            return "";
        }
        
        if (!currentSpan.getOperationName().equals(layer)) {
            logger.warn("Mismatching span name on entry and exit trace event, entry event: " + currentSpan.getOperationName() + " ; exit event: " + layer);
        }
        
        
        if (info != null) {
            for (Entry<String, Object> entry : info.entrySet()) {
                currentSpan.setTagAsObject(entry.getKey(), entry.getValue());
            }
        }
        currentSpan.finish(); //Scope started by SDK uses the default OT behavior - that finishOnClose for the wrapped span is `false`
        
        Long spanThreadId = currentSpan.getSpanPropertyValue(SpanProperty.THREAD_ID);
        if (spanThreadId != null && spanThreadId == Thread.currentThread().getId()) { //if starting and ending trace on the same thread, close the scope
            currentScope.close();
        } else { //otherwise let whoever propagates the context handle the scope (usually ContextPropagationPatcher) 
            logger.debug("Exiting trace on different thread. Trace started at " + spanThreadId + " but ending at " + Thread.currentThread().getId());
        }
        
        return currentSpan.getSpanPropertyValue(SpanProperty.EXIT_XID); 
    }

    /**
     * {@inheritDoc}
     */
    public String endTrace(String layer, Object... info) {
        return endTrace(layer, Utils.keyValuePairsToMap(info));
    }

   
    /**
     * {@inheritDoc}
     */
    public TraceEvent createEntryEvent(String layer) {
        return createEvent(layer, "entry");
    }

    /**
     * {@inheritDoc}
     */
    public TraceEvent createExitEvent(String layer) {
        return createEvent(layer, "exit");
    }

    /**
     * {@inheritDoc}
     */
    public TraceEvent createInfoEvent(String layer) {
        return createEvent(layer, "info");
    }

    /**
     * {@inheritDoc}
     */
    public void logException(Throwable error) {
        Event event = Context.createEvent();

        event.addInfo("Label", "error",
                      "ErrorClass", error.getClass().getName(),
                      "ErrorMsg" , error.getMessage());
        ClassInstrumentation.addBackTrace(event, 1);

        event.report();
    }

    /**
     * {@inheritDoc}
     */
    public String getCurrentXTraceID() {
        return Context.getMetadata().toHexString();
    }
    
    @Override
    public String getCurrentLogTraceId() {
        try {
            return Context.getMetadata().getCompactTraceId();
        } catch (NoSuchMethodError e) {
            logger.warn("Cannot get the trace id for log, require agent verion 6.10.0 or later : " + e.getMessage());
            return "";
        }
    }

    private TraceEvent createEvent(String layer, String eventType) {
        if (!Context.getMetadata().isSampled()) {
            return NO_OP;
        }

        Event event = Context.createEvent();
        event.addInfo("Layer", layer, "Label", eventType);
        return event == null ? NO_OP : new EventWrapper(event);
    }

    private TraceEvent startOrContinueTrace(String layer, String inXTraceID) {
        return new StartTraceEvent(layer, inXTraceID);
    }
    
    public boolean setTransactionName(String transactionName) {
        if (transactionName == null || "".equals(transactionName)) {
            return false;   
        }
        
        boolean isSet = Tracer.setTraceProperty(TraceProperty.CUSTOM_TRANSACTION_NAME, transactionName);
        if (!isSet) {
            logger.debug("Transaction name [" +  transactionName + "] is ignored as current execution flow is not monitored by the agent");
        }
        return isSet;
    }

    private static final NoOpEvent NO_OP = new NoOpEvent();
}
