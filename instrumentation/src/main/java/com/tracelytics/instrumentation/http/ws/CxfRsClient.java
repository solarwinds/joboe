package com.tracelytics.instrumentation.http.ws;

/**
 * Wrapper class of org.apache.cxf.jaxrs.client.AbstractClient to trace the URI, Http Method and Http response header (x-trace ID)
 * @author Patson Luk
 *
 */
public interface CxfRsClient {
    String getUri();
    String getHttpMethod();
    String getXTraceId();
}
