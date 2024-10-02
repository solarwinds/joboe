package com.tracelytics.instrumentation.http.webflow;

/**
 * Wrapper class for known Spring action that can be expressed as an expression string
 * For example <code>org.springframework.webflow.action.EvaluateAction</code> and <code>org.springframework.webflow.action.SetAction</code>
 * @author Patson Luk
 *
 */
public interface ExpressionAction {
    /**
     * 
     * @return the expression used in the Action as a String. For implementation, please refer to <code>ActionInstrumentation</code>
     */
    String getExpressionString();
}
