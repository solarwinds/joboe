package com.tracelytics.instrumentation.http.ws;


public interface PlayScalaWsResponse {
    String tvGetXTraceHeaderValue();
    
    int status(); //existing method, just add it here so we can call it conveniently
    String statusText(); //existing method, just add it here so we can call it conveniently
}
