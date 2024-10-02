package com.tracelytics.instrumentation.http.akka.server;

public interface AkkaHttpRequestStart {
    String tvUriPath();
    int tvUriPort();
    String tvUriHost();
    String tvHttpMethod();
    AkkaHttpRequestStart tvWithHeader(String headerKey, String headerValue);
    String tvGetHeader(String key);
    String tvQuery();
    String tvScheme();

}