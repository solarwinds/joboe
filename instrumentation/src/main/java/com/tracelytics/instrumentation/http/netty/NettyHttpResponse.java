package com.tracelytics.instrumentation.http.netty;

public interface NettyHttpResponse extends NettyHttpMessage {
    void tvSetHeader(String s, Object o);
    String tvGetHeader(String name);

    Integer tvGetStatusCode();
}
