package com.solarwinds.joboe.core.span.propagation;

import com.solarwinds.joboe.core.span.SpanContext;

public interface Injector<T> {
    void inject(SpanContext spanContext, T carrier);
}