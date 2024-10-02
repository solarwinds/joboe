package com.tracelytics.instrumentation.http.netty;

public interface NettyHttpRequest extends NettyHttpMessage {
    void tvSetHeader(String s, Object o);
    String tvGetHeader(String name);

    String getUri();
    String getTvMethod();
}
