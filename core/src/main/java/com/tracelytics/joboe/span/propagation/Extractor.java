package com.tracelytics.joboe.span.propagation;

import com.tracelytics.joboe.span.SpanContext;

public interface Extractor<T> {
    SpanContext extract(T carrier);
}