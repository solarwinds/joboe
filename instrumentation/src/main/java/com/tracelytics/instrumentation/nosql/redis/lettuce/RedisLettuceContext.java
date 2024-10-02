package com.tracelytics.instrumentation.nosql.redis.lettuce;

import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Store values related to Lettuce Context such as current ongoing Lettuce dispatch
 * @author Patson Luk
 *
 */
class RedisLettuceContext {
    static final ThreadLocal<Metadata> dispatchContext = new ThreadLocal<Metadata>();
    
    static void setActiveDispatch() {
        dispatchContext.set(Context.getMetadata());      
    }
    static Metadata getActiveDispatch() {
        return dispatchContext.get();
    }
    static void unsetActiveDispatch() {
        dispatchContext.remove();
    }
}
