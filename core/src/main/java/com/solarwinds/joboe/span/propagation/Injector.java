package com.solarwinds.joboe.span.propagation;

import com.solarwinds.joboe.span.SpanContext;

public interface Injector<T> {
    void inject(SpanContext spanContext, T carrier);
}