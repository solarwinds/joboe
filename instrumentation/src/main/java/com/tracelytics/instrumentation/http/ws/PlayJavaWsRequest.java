package com.tracelytics.instrumentation.http.ws;


public interface PlayJavaWsRequest {
    Object tvSetHeader(String headerKey, String headerValue); //it has a setHeader method but it returns the Request object...
    
    String getUrl(); //existing method, just add it here so we can call it conveniently
    String getMethod(); //existing method for 2.2 and 2.3, if 2.4 does not have it, then add one
}
