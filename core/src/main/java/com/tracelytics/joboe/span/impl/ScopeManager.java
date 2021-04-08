package com.tracelytics.joboe.span.impl;

import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

public class ScopeManager implements com.tracelytics.joboe.span.ScopeManager {
    private Logger logger = LoggerFactory.getLogger();

    public static final ScopeManager INSTANCE = new ScopeManager();
    private static ScopeContext scopeContext = new ThreadLocalScopeContext();

    private ScopeManager() {
    }

    @Override
    public Scope active() {
        return scopeContext.getCurrentScope();
    }

    @Override
    public Scope activate(com.tracelytics.joboe.span.Span span) {
        return activate(span, false);
    }

    @Override
    public Scope activate(com.tracelytics.joboe.span.Span span, boolean finishOnClose) {
        return activate(span, finishOnClose, false);
    }

    public Scope activate(com.tracelytics.joboe.span.Span span, boolean finishOnClose, boolean asyncActivate) {
        if (!(span instanceof Span)) {
            logger.warn("Only support activating span that is [" + Span.class.getName() + " but found [" + span.getClass().getName() + "]");
            return null;
        }
        Scope scope = new Scope((Span) span, finishOnClose, asyncActivate);

        //since this span is activated, make sure the context is sync with the TLS context
        if (asyncActivate) { //this span is activated from another async flow, create a clone and mark as async
            Metadata metadata = new Metadata(((Span) span).context().getMetadata());
            metadata.setIsAsync(true);
            Context.setMetadata(metadata);
        } else {
            Context.setMetadata(((Span) span).context().getMetadata());
        }

        return scope;
    }

    @Override
    public Span activeSpan() {
        Scope scope = active();
        return scope != null ? scope.span() : null;
    }
    
    void addScope(Scope scope) {
        scopeContext.addScope(scope);
    }
    
    Scope removeScope() {
        Scope removedScope = scopeContext.removeScope();
        if (removedScope != null && Context.getMetadata() == removedScope.span().context().getMetadata()) { //only reset if current context is the same instance as this span's metadata
            //reset the metadata to previous value
            Metadata previousMetadata = removedScope.getPreviousMetadata();
            if (previousMetadata != null) {
                Context.setMetadata(previousMetadata);
            } else {
                Context.clearMetadata();
            }
        }

        return removedScope;
    }

    public int removeAllScopes() {
        Scope scope = removeScope();
        int count = 0;
        while (scope != null) {
            scope = removeScope();
            count ++;
        }
        return count;
    }

    public ScopeContextSnapshot getSnapshot() {
        return scopeContext.getSnapshot();
    }

    public int getScopeCount() {
        return scopeContext.getScopeCount();
    }
}