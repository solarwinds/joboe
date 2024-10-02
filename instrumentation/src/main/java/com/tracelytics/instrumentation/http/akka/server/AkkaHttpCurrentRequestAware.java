package com.tracelytics.instrumentation.http.akka.server;

/**
 * Interface to provide the active `RequestStart` of current request handling
 */
public interface AkkaHttpCurrentRequestAware {
    AkkaHttpRequestStart tvGetCurrentRequest();

}