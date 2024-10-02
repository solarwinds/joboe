package com.appoptics.api.ext.impl;

import com.appoptics.api.ext.TraceContext;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.span.impl.ScopeContextSnapshot;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

public class TraceContextHandler implements ITraceContextHandler {
    private final Logger logger = LoggerFactory.getLogger();
    /**
     * Returns the Context currently associated with this thread.
     *
     * Note that this context is a copy of the TLS context: modifications will NOT affect the current
     * thread unless setAsDefault is called.
     *
     * @return ITraceContextHandler
     */
    public TraceContext getDefault() {
     // Note we are cloning the metadata:
        Metadata md = new Metadata(Context.getMetadata());
        
        return new TraceContextConcrete(md, ScopeManager.INSTANCE.getSnapshot());
    }

    /**
     * Resets the current thread's context (updates TLS)
     */
    public void clearDefault() {
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
    }
    
    @Override
    public boolean isSampled(String xTraceID) {
        try {
            return new Metadata(xTraceID).isSampled();
        } catch (OboeException e) {
            logger.warn("X-Trace ID [" + xTraceID + "] is not valid");
            return false;
        }
    }
    
    
    class TraceContextConcrete extends TraceContext {
        private final Metadata md;
        private final ScopeContextSnapshot scopeContextSnapshot;

        /**
         * Constructor: Currently, the only way public way to obtain a ITraceContextHandler is through getDefault
         */
        protected TraceContextConcrete(Metadata md, ScopeContextSnapshot scopeContextSnapshot) {
            this.md = md;
            this.scopeContextSnapshot = scopeContextSnapshot;
        }
        
        @Override
        /**
         * Sets the current thread's context to this context (updates TLS)
         * 
         * Take note that if current thread does not invoke {@link com.appoptics.api.ext.Trace#endTrace} to end the trace,
         * then it is encouraged to invoke {@link TraceContext#clearDefault()} to clear up the context after the processing is done on current thread.
         */
        public void setAsDefault() {
          //only propagate root span for now as it's the only one that "requires" propagation, all non root span should work fine with legacy Metadata
            if (scopeContextSnapshot != null) {
              //this prevent scope leaking, as scope could leak from last call if the trace was not ended properly
              ScopeManager.INSTANCE.removeAllScopes();
              scopeContextSnapshot.restore();
            }
            Context.setMetadata(new Metadata(md));
        }
    }
}
