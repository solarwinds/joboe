package com.tracelytics.joboe.span.propagation;

import com.tracelytics.joboe.span.SpanContext;

public interface Injector<T> {
    void inject(SpanContext spanContext, T carrier);
}