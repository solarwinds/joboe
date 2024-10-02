package com.tracelytics.instrumentation.http.webflow;

/**
 * Wrapper for <code>org.springframework.webflow.execution.RequestContext</code>
 * @author Patson Luk
 *
 */
public interface RequestContext {
    
    /**
     * 
     * @return the expression of the current transition used for the State enter/exit
     */
    String getTransitionOn();
    
    /**
     *
     * @return the current state as an Object, take note that we cannot use the getCurrentState() method directly as it returns a <code>org.springframework.webflow.definition.StateDefinition</code>
     * object which we cannot refer to from our code
     */
    Object getCurrentStateObject();
}
