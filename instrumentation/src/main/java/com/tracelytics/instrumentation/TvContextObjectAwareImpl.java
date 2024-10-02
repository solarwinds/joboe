package com.tracelytics.instrumentation;

import com.tracelytics.joboe.Metadata;

public class TvContextObjectAwareImpl implements TvContextObjectAware {
    private Metadata contextObject;
    private Metadata clonedContext;
    private Metadata previousContextObject;
    private long fromThreadId;
    private boolean restored;
    
     
    public void setTvContext(Metadata metadata) {
        this.contextObject = metadata;
    }

    public Metadata getTvContext() {
        return contextObject;
    }

    public void setTvPreviousContext(Metadata previousContext) {
        this.previousContextObject = previousContext;
    }

    public Metadata getTvPreviousContext() {
        return previousContextObject;
    }
    
    @Override
    public void tvSetClonedContext(Metadata clonedContext) {
        this.clonedContext = clonedContext;
    }
    
    @Override
    public Metadata tvGetClonedContext() {
        return clonedContext;
    }

    public void setTvFromThreadId(long threadId) {
        this.fromThreadId = threadId;
    }

    public long getTvFromThreadId() {
        return fromThreadId;
    }

    public void setTvRestored(boolean restored) {
        this.restored = restored;
    }

    public boolean tvRestored() {
        return restored;
    }
}