package com.tracelytics.instrumentation.http.ws;

/**
 * Wrapper for the org.apache.cxf.jaxrs.client.JaxrsClientCallback$JaxrsResponseCallback. Added a handle to get the clientCallback added in the ctor
 * @author Patson Luk
 *
 */
public interface CxfRsResponseCallback {
    Object getClientCallback();
    
}
