package com.tracelytics.instrumentation.http.ws;

public interface SunJerseyClientRequest {
    void setAsync(boolean isAsync);
    boolean isAsync();
}
