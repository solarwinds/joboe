package com.solarwinds.joboe.span.impl;

import com.solarwinds.joboe.Context;
import com.solarwinds.joboe.Metadata;
import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;

public class ScopeManager implements com.solarwinds.joboe.span.ScopeManager {
    private final Logger logger = LoggerFactory.getLogger();

    public static final ScopeManager INSTANCE = new ScopeManager();
    private static final ScopeContext scopeContext = new ThreadLocalScopeContext();
    private static ScopeListener listener;

    public interface ScopeListener {
        void onAddScope(Scope scope);
        void onRemoveScope(Scope scope);
    }

    private ScopeManager() {
    }

    @Override
    public Scope active() {
        return scopeContext.getCurrentScope();
    }

    @Override
    public Scope activate(com.solarwinds.joboe.span.Span span) {
        return activate(span, false);
    }

    public static void registerListener(ScopeListener listener) {
        ScopeManager.listener = listener;
    }

    @Override
    public Scope activate(com.solarwinds.joboe.span.Span span, boolean finishOnClose) {
        return activate(span, finishOnClose, false);
    }

    public Scope activate(com.solarwinds.joboe.span.Span span, boolean finishOnClose, boolean asyncActivate) {
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
        if (!scope.span().getSpanPropertyValue(Span.SpanProperty.IS_SYNC_SPAN) && listener != null) {
            listener.onAddScope(scope);
        }
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
        if (listener != null && removedScope != null && !removedScope.span().getSpanPropertyValue(Span.SpanProperty.IS_SYNC_SPAN)) {
            listener.onRemoveScope(removedScope);
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