package com.appoptics.api.ext.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.appoptics.api.ext.TraceEvent;
import com.appoptics.api.ext.impl.Utils;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.Tracer.SpanBuilder;

/**
 * Created by Trace.startTrace() call, enable creating a root Span under the hood from SDK calls 
 * as root span is essential for inbound-metrics reporting
 * 
 * The span is created on `StartTraceEvent.report`
 *  
 * @author Patson
 *
 */
public class StartTraceEvent implements TraceEvent {
    private Map<String, Object> keyValues = new HashMap<String, Object>();
    private StackTraceElement[] backTrace;
    private boolean isAsync;
    private final String layer;
    private final String inXTraceID; 
    
    public StartTraceEvent(String layer, String inXTraceID) {
        this.layer = layer;
        this.inXTraceID = inXTraceID;
    }

    public void addInfo(String key, Object value) {
        keyValues.put(key, value);
    }

    public void addInfo(Map<String, Object> infoMap) {
        keyValues.putAll(infoMap);
    }

    public void addInfo(Object... info) {
        keyValues.putAll(Utils.keyValuePairsToMap(info));
    }

    public void setAsync() {
        isAsync = true;
    }

    public void addEdge(String xTraceID) {
        //should not add edge in start event, for continuing trace, it should call Trace.continueTrace instead
    }

    public void report() {
        SpanBuilder spanBuilder = ClassInstrumentation.getStartTraceSpanBuilder(layer, inXTraceID != null ? Collections.singletonMap(XTraceHeader.TRACE_ID, inXTraceID) : Collections.<XTraceHeader, String>emptyMap(), null);
        spanBuilder.withTags(keyValues);
        spanBuilder.withSpanProperty(SpanProperty.THREAD_ID, Thread.currentThread().getId());
        Span span = spanBuilder.start();
        ScopeManager.INSTANCE.activate(span);
        
        //Take note that below will show on the EXIT event of the trace. Though not as the most desirable behavior, this avoids a lot of complexity of passing values to entry event
        if (isAsync) {
            span.setSpanPropertyValue(SpanProperty.IS_ASYNC, true);
        }
        if (backTrace != null) {
            ClassInstrumentation.addBackTrace(span, backTrace);
        }
        span.setSpanPropertyValue(SpanProperty.IS_SDK, true);
    }

    public void addBackTrace() {
        backTrace = ClassInstrumentation.getBackTrace(1);
    }
    
    
}
