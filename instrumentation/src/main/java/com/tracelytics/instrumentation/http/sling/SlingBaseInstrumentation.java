package com.tracelytics.instrumentation.http.sling;

import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Abstract base class for Sling instrumentation
 * @author Patson Luk
 *
 */
public abstract class SlingBaseInstrumentation extends ClassInstrumentation {
    protected static final String LAYER_NAME = "sling";
    
    
    private static ThreadLocal<Integer> depthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };
    

    /**
     * Checks whether the current instrumentation should start a new extent. If there is already an active extent, then do not start one
     * @return
     */
    protected static boolean shouldStartExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth + 1);

        if (currentDepth == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether the current instrumentation should end the current extent. If this is the active extent being traced, then ends it
     * @return
     */
    protected static boolean shouldEndExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth - 1);

        if (currentDepth == 1) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Returns whether there are any active extent
     * @return
     */
    static boolean hasActiveExtent() {
        return depthThreadLocal.get() != null && depthThreadLocal.get() > 0;
    }
    
  
    
}
