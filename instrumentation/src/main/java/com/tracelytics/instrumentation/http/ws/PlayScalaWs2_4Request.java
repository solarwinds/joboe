package com.tracelytics.instrumentation.http.ws;


public interface PlayScalaWs2_4Request {
    String getTvGeneratedMetadata();
    void setTvGeneratedMetadata(String generatedMetadata);
    
    PlayScalaWs2_4Request tvWithXTraceHeader(String metadata);
    
    String url(); //existing method, just add it here so we can call it conveniently
    String method(); //existing method, just add it here so we can call it conveniently
}
