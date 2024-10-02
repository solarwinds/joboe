package com.tracelytics.instrumentation.http.webflow;

/**
 * Wrapper class for <code>org.springframework.webflow.execution.AnnotatedAction</code>
 * @author Patson Luk
 *
 */
public interface AnnotatedAction {
    
    /**
     * 
     * @return the action as readable String. For implementation, please refer to <code>ActionInstrumentation</code>
     */
    String getTargetActionString();
}
