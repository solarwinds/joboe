package com.solarwinds.joboe.span.propagation;

import com.solarwinds.joboe.span.SpanContext;

public interface Extractor<T> {
    SpanContext extract(T carrier);
}