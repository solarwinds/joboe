package com.tracelytics.joboe.span.context;

import com.tracelytics.joboe.span.impl.ActiveSpan;
import com.tracelytics.joboe.span.impl.BaseSpan.TraceProperty;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Tracer;

/**
 * Deprecated, should use {@link ScopeManager directly} - Manager that provides method to operate on the TraceContext of this java process
 * 
 * @deprecated
 * @author pluk
 *
 */
public class ContextManager {
    private ContextManager() {
        //utils class, disallow instantiation
    }
    
    
    public static int removeAllSpans() {
        return ScopeManager.INSTANCE.removeAllScopes();
    }

    public static ActiveSpan getCurrentSpan() {
        return new ActiveSpan(ScopeManager.INSTANCE.active());
    }
    
    /**
     * Sets the `TraceProperty` when there's no available `Span` object
     * 
     * The code would look up the current active `Metadata` (which is more likely be available), look up 
     * and set the corresponding `TraceProperty` with the provided value
     *  
     * @param traceProperty
     * @param value
     * @return  true if a there's a valid `Metadata` and that the property was set successfully
     */
    public static <T> boolean setTraceProperty(TraceProperty<T> traceProperty, T value) {
        return Tracer.setTraceProperty(traceProperty.getWrapped(), value);
    }
}
