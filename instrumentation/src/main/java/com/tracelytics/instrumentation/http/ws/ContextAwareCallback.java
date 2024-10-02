package com.tracelytics.instrumentation.http.ws;

public interface ContextAwareCallback {
    String getTvContext();
    void setTvContext(String context);
}
