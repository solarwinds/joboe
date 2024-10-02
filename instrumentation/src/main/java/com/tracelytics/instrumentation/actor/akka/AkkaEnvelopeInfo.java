package com.tracelytics.instrumentation.actor.akka;

import com.tracelytics.instrumentation.TvContextObjectAwareImpl;
import com.tracelytics.joboe.Metadata;

class AkkaEnvelopeInfo extends TvContextObjectAwareImpl {
    private long creationTimestamp;
    private Metadata entryContext; 
    
     
    public AkkaEnvelopeInfo() {
        this.creationTimestamp = System.nanoTime();
    }
    
    long getCreationTimestamp() {
        return creationTimestamp;
    }
    
    
    /**
     * A fallback for Akka actors, as the actor operation intertwined with other frameworks (such as Spray), sometimes the trace is ended before the envelope exit.
     * 
     * In order to ensure we can always exit properly on akka actors, we will keep track of the entry context here
     * @return
     */
    public Metadata getEntryContext() {
        return entryContext; 
    }
    
    public void setEntryContext(Metadata entryContext) {
        this.entryContext = entryContext;
    }
}