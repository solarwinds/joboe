package com.tracelytics.instrumentation;

import com.tracelytics.joboe.span.impl.Span;

public class SpanAwareImpl implements SpanAware {
    private Span span;
    
    public void tvSetSpan(Span span) {
        this.span = span;
    }

    public Span tvGetSpan() {
        return span;
    }
}
