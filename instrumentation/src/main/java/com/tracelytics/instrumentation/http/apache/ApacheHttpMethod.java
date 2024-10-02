package com.tracelytics.instrumentation.http.apache;

/**
 * Tagged interface for org.apache.commons.httpclient.HttpMethod
 */
public interface ApacheHttpMethod {
    void setRequestHeader(String headerName, String headerValue);
}