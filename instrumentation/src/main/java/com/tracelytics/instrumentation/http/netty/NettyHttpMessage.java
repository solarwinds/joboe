package com.tracelytics.instrumentation.http.netty;

public interface NettyHttpMessage {
    void tvSetHeader(String s, Object o);
    String tvGetHeader(String name);
}
