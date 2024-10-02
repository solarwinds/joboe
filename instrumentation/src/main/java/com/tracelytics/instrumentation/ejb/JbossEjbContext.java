package com.tracelytics.instrumentation.ejb;

import com.tracelytics.joboe.Metadata;

/**
 * Used as a container for keeping track of various context information
 * @author Patson Luk
 *
 */
public class JbossEjbContext {
    private static ThreadLocal<String> responseXTraceId = new ThreadLocal<String>();
    private static ThreadLocal<Metadata> asyncContext = new ThreadLocal<Metadata>();
    
    
    static String getResponseXTraceId() {
        return responseXTraceId.get();
    }
    
    static void setResponseXTraceId(String xTraceId) {
        responseXTraceId.set(xTraceId);
    }
    
    /**
     * Get the context for the JBoss ejb async extent if available
     * @return
     */
    static Metadata getAsyncContext() {
        return asyncContext.get();
    }
    
    /**
     * Set the context for the JBoss ejb async extent 
     * @param context
     */
    static void setAsyncContext(Metadata context) {
        asyncContext.set(context);
    }
}
