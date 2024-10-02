package com.tracelytics.instrumentation;

import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.span.impl.Span;

public class ContextObjectAndSpanAwareImpl implements TvContextObjectAware, SpanAware {
    private Metadata contextObject;
    private Metadata clonedContext;
    private Metadata previousContextObject;
    private Span span;
    private long fromThreadId;
    private boolean contextRestored;
    
     
    public void setTvContext(Metadata metadata) {
        this.contextObject = metadata;
    }

    public Metadata getTvContext() {
        return contextObject;
    }
    
    @Override
    public void tvSetClonedContext(Metadata clonedContext) {
        this.clonedContext = clonedContext;
    }
    
    @Override
    public Metadata tvGetClonedContext() {
        return clonedContext;
    }

    public void setTvPreviousContext(Metadata previousContext) {
        this.previousContextObject = previousContext;
    }

    public Metadata getTvPreviousContext() {
        return previousContextObject;
    }

    public void setTvFromThreadId(long threadId) {
        this.fromThreadId = threadId;
    }

    public long getTvFromThreadId() {
        return fromThreadId;
    }

    public void setTvRestored(boolean restored) {
        this.contextRestored = restored;
    }

    public boolean tvRestored() {
        return contextRestored;
    }
    
    @Override
    public void tvSetSpan(Span span) {
        this.span = span;
    }

    @Override
    public Span tvGetSpan() {
        return span;
    }
}