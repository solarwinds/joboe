package com.solarwinds.joboe.span.impl;

import com.solarwinds.joboe.Context;
import com.solarwinds.joboe.Metadata;
import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;
import lombok.Getter;

public class Scope implements com.solarwinds.joboe.span.Scope  {
    private final Span wrapped;

    private final Logger logger = LoggerFactory.getLogger();
    private final boolean finishOnClose;
    private final boolean isAsyncPropagation;
    @Getter
    private final Metadata previousMetadata;
    
    private static final ScopeManager scopeManager = ScopeManager.INSTANCE;

    Scope(Span wrapped, boolean finishOnClose, boolean isAsyncPropagation) {
        this.wrapped = wrapped;
        this.finishOnClose = finishOnClose;
        this.isAsyncPropagation = isAsyncPropagation;
        //keep track of current metadata so when scope is removed, it can revert to existing metadata
        //take note that we should not use wrapped.context().getPreviousMetadata() as that one could be
        //parent span's metadata, which the parent span is not necessary the active span
        this.previousMetadata = Context.getMetadata();

        scopeManager.addScope(this);
    }
    

    @Override
    public void close() {
        Scope activeScope = scopeManager.active();
        if (activeScope != this) {
            logger.warn("deactivate scope with span " + span().getOperationName() + " but found unmatching active scope with span " + activeScope.span().getOperationName());
            return;
        }
        scopeManager.removeScope();
        
        if (finishOnClose) {
            wrapped.finish();
        }
    }

    @Override
    public com.solarwinds.joboe.span.impl.Span span() {
        return wrapped;
    }
    

    /**
     * Gets whether this scope was created/activated from a span originates from a different thread or in an asynchronous manner
     * @return
     */
    public boolean isAsyncPropagation() {
        return isAsyncPropagation;
    }


}