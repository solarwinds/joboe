package com.tracelytics.instrumentation;

import com.tracelytics.joboe.span.impl.Span;

/**
 * A carrier to store context of both the legacy Metadata and the newer OT `Span` object
 */
public class TraceContextData
        extends TvContextObjectAwareImpl implements SpanAware {
    private SpanAware spanData;
    
    public TraceContextData() {
        super();
        spanData = new SpanAwareImpl();
    }
    
    @Override
    public void tvSetSpan(Span span) {
        spanData.tvSetSpan(span);
    }
    
    @Override
    public Span tvGetSpan() {
        return spanData.tvGetSpan();
    }
    
}
