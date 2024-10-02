package com.tracelytics.instrumentation.http.ws;


public interface PlayScalaWs2_3MinusRequest {
    Object tvSetHeader(String headerKey, String headerValue); //it has a setHeader method but it returns the Request object...
    
    String url(); //existing method, just add it here so we can call it conveniently
    String method(); //existing method, just add it here so we can call it conveniently
}
