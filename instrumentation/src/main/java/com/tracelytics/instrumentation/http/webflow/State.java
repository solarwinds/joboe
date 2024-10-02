package com.tracelytics.instrumentation.http.webflow;

/**
 * Wrapper for <code>org.springframework.webflow.engine.State</code>
 * @author Patson Luk
 *
 */
public interface State {
    /**
     * @return the Id of the State
     */
	String getId();
	
	/**
	 * 
	 * @return the Id of the Flow which the State is in
	 */
	String getFlowId();
}
