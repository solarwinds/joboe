package com.tracelytics.instrumentation.http.grizzly;

import com.tracelytics.instrumentation.SpanAware;

public interface GlassfishGrizzlyResponse extends SpanAware {
    int getStatus();

    void setHeader(String key, String value);
}
