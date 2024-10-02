package com.tracelytics.instrumentation.http.ws;

public interface ResteasyClientInvocation {
    void setAsync(boolean isAsync);
    boolean isAsync();
}
