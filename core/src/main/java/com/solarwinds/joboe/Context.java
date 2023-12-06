/**
 * Associates metadata with current thread
 */
package com.solarwinds.joboe;

import com.solarwinds.joboe.settings.SettingsArg;
import com.solarwinds.joboe.settings.SettingsArgChangeListener;
import com.solarwinds.joboe.settings.SettingsManager;
import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;

public class Context {
    private static final ThreadLocal<Boolean> skipInheritingContextThreadLocal = new ThreadLocal<Boolean>();
    private static final Logger logger = LoggerFactory.getLogger();
    private static boolean inheritContext = true; //whether child thread should inherits metadata (clone) from parent
    
    // Thread local storage for metadata. This is inheritable so that child threads pick up the parent's context.
    private static final InheritableThreadLocal<Metadata> mdThreadLocal = new InheritableThreadLocal<Metadata>() {
        @Override 
        protected Metadata initialValue() {
            Metadata md = new Metadata();
            return md;
        }

        @Override
        protected Metadata childValue(Metadata parentMetadata) {
            if (!inheritContext || (skipInheritingContextThreadLocal.get() != null && skipInheritingContextThreadLocal.get())) {
                return new Metadata(); //do not propagate context here, return an empty context
            } else { 
                Metadata clonedMetadata = new Metadata(parentMetadata);
              //if parent span is sampled, that means a parent span exists then this is a child span spawn off from a thread from parent span, mark this as asynchronous
                if (parentMetadata.isSampled()) { 
                    clonedMetadata.setIsAsync(true);
                }
                return clonedMetadata;
            }
        }
    };
    
    static {
        SettingsManager.registerListener(new SettingsArgChangeListener<Boolean>(SettingsArg.DISABLE_INHERIT_CONTEXT) { //listen to Settings change on inheriting context
            @Override
            public void onChange(Boolean newValue) {
                if (newValue != null) {
                    inheritContext = !newValue;
                } else {
                    inheritContext = true; //by default we inherit context
                }
            }
        });
    }

    public static Event startTrace() {
        getMetadata().randomize(true);
        return createEventWithContext(getMetadata(), false); //do not add edge on trace start
    }

    public static Event createEvent() {
        return createEventWithContext(getMetadata(), true);
    }
    
    public static Event createEventWithID(String metadataID)
        throws OboeException {
        return createEventWithIDAndContext(metadataID, getMetadata());
    }
    
    public static Event createEventWithContext(Metadata context) {
    	return createEventWithContext(context, true); //by default add edge (not trace start)
    }
    
    public static Event createEventWithContext(Metadata context, boolean addEdge) {
        if (shouldCreateEvent(context)) {
            return new EventImpl(context, addEdge); 
        } else {
            return new NoopEvent(context);
        }
    }
    
    public static Event createEventWithIDAndContext(String metadataID, Metadata currentContext) throws OboeException {
        if (shouldCreateEvent(currentContext)) {
            return new EventImpl(currentContext, metadataID, true);
        } else {
            return new NoopEvent(currentContext);
        }
        
    }

    public static Event createEventWithGeneratedMetadata(Metadata generatedMetadata) {
        return createEventWithGeneratedMetadata(null, generatedMetadata);
    }

    public static Event createEventWithGeneratedMetadata(Metadata parentMetadata, Metadata generatedMetadata) {
        if (shouldCreateEvent(generatedMetadata)) {
            return new EventImpl(parentMetadata, generatedMetadata);
        } else {
            return new NoopEvent(generatedMetadata);
        }
    }
    
    private static boolean shouldCreateEvent(Metadata context) {
        if (!context.isSampled()) {
            return false;
        } 
        
        if (!context.incrNumEvents()) {
            //Should not invalidate as metrics should still be captured, setting it to not sampled might also impact logic that assume a "sampled" entry point should match a "sampled" exit point
            return false;
        } 
        
        if (context.isExpired(System.currentTimeMillis())){
            //Consider an error condition (context leaking) Hence we should expire the context metadata to stop further processing/leaking
            context.invalidate(); 
            return false;
        }
        
        return true;
    }

    /**
     * Returns metadata for current thread
     */
    public static Metadata getMetadata() {
        return mdThreadLocal.get();
    }
    
    /**
     * Sets  metadata for current thread
     * @param md
     */
    public static void setMetadata(Metadata md) {
        setMap(md);
    }

    /**
     *  Sets metadata for this thread from
     * @param hexStr
     * @throws OboeException
     */
    public static void setMetadata(String hexStr) throws OboeException {
        Metadata md = new Metadata();
        md.fromHexString(hexStr);
        setMap(md);
    }

    /**
     * Clears metadata for current thread
     */
    public static void clearMetadata() {
        setMap(new Metadata());
    }
    
    private static void setMap(Metadata md) {
        mdThreadLocal.set(md);
    }

    public static boolean isValid() {
        return getMetadata().isValid();
    }

    /**
     * Set whether the any child threads spawned by the current thread should skip inheriting a clone of current context
     * 
     * It is known that in thread pool handling, using inheritable thread local has problem of leaking context. (unable to clear the context in the spawned thread afterwards) 
     * This can be set to true if the context propagation is handled by other means in order to avoid the leaking problem.
     * 
     * By default this is false
     * 
     * @param skipInheritingContext
     */
    public static void setSkipInheritingContext(boolean skipInheritingContext) {
        skipInheritingContextThreadLocal.set(skipInheritingContext);
    }
}
