package com.tracelytics.instrumentation.http.apache;

/**
 * Tagged interface for org.apache.http.HttpMessage (Apache Http Client v4.x)
 */
public interface ApacheHttpMessage {
    void setHeader(String header, String value);
}