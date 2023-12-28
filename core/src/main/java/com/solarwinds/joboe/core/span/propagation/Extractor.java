package com.solarwinds.joboe.core.span.propagation;

import com.solarwinds.joboe.core.span.SpanContext;

public interface Extractor<T> {
    SpanContext extract(T carrier);
}