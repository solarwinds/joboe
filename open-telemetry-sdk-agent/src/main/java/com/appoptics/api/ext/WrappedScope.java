package com.appoptics.api.ext;

import io.grpc.Context;
import io.opentelemetry.context.Scope;

class WrappedScope implements Scope {
    private final ContextChangeListener listener;
    private final Scope sdkScope;

    WrappedScope(Scope sdkScope, ContextChangeListener listener) {
        this.sdkScope = sdkScope;
        this.listener = listener;
    }
    
    @Override
    public void close() {
        Context oldContext = Context.current();
        sdkScope.close();
        if (listener != null) {
            listener.onContextChange(oldContext, Context.current());
        }
    }
}
