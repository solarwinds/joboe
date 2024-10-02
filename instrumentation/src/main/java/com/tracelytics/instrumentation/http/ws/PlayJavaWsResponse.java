package com.tracelytics.instrumentation.http.ws;


public interface PlayJavaWsResponse {
    String getHeader(String key);
    
    int getStatus(); //existing method, just add it here so we can call it conveniently
    String getStatusText(); //existing method, just add it here so we can call it conveniently
}
