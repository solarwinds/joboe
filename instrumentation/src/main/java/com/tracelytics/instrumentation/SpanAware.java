package com.tracelytics.instrumentation;

import com.tracelytics.joboe.span.impl.Span;

public interface SpanAware {
    void tvSetSpan(Span span);
    Span tvGetSpan();
}
