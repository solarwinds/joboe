package com.tracelytics.joboe.span.impl;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

public class Scope implements com.tracelytics.joboe.span.Scope  {
    private final Span wrapped;

    private Logger logger = LoggerFactory.getLogger();
    private final boolean finishOnClose;
    private final boolean isAsyncPropagation;
    
    private static final ScopeManager scopeManager = ScopeManager.INSTANCE;

    Scope(Span wrapped, boolean finishOnClose, boolean isAsyncPropagation) {
        this.wrapped = wrapped;
        this.finishOnClose = finishOnClose;
        this.isAsyncPropagation = isAsyncPropagation;
        
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
    public com.tracelytics.joboe.span.impl.Span span() {
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