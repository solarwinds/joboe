package com.tracelytics.instrumentation;

import com.tracelytics.joboe.Metadata;

public interface TvContextObjectAware {
    void setTvContext(Metadata metadata);
    Metadata getTvContext();

    void tvSetClonedContext(Metadata clonedContext);
    Metadata tvGetClonedContext();
    
    void setTvPreviousContext(Metadata previousContext);
    Metadata getTvPreviousContext();
    
    void setTvFromThreadId(long threadId);
    long getTvFromThreadId();
    
    void setTvRestored(boolean restored);
    boolean tvRestored();
}
