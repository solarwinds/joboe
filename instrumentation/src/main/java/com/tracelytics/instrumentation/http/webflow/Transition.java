package com.tracelytics.instrumentation.http.webflow;

/**
 * Wrapper for <code>org.springframework.webflow.engine.Transition</code>
 * @author Patson Luk
 *
 */
public interface Transition {
    
    /**
     * 
     * @return the expression used in the evaluation, for implementation please refer to <code>TransitionInstrumentation</code>
     */
	String getMatchingCriteriaString();
	
	/**
	 * 
	 * @return whether the last source state used in this transition was a Decision State
	 */
	boolean isFromDecisionState();
	
	/**
	 * Set the field isFromDecisionState
	 * 
	 * @param isFromDecisionState whether the last source state used in this transition was a Decision State
	 */
	void setFromDecisionState(boolean isFromDecisionState);
}
