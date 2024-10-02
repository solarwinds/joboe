package com.tracelytics.instrumentation.http.ws;

public interface CxfClientCallback {
    /**
     * 
     * @return layer name related to this callback 
     */
    String tvGetLayer();
    void tvSetLayer(String layer);
}
