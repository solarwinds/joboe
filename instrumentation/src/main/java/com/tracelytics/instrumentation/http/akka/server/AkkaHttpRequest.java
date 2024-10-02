package com.tracelytics.instrumentation.http.akka.server;

public interface AkkaHttpRequest {
    String tvPath();
    int tvPort();
    String tvHost();
    String tvHttpMethod();
    AkkaHttpRequest tvWithHeader(String headerKey, String headerValue);
    String tvGetHeader(String key);
    String tvQuery();
    String tvScheme();

}