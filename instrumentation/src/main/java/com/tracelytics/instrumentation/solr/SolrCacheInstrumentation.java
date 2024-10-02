package com.tracelytics.instrumentation.solr;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instrumentation on the base class RequestHandlerBase of all Solr handlers. Solr applies handler to handle each query request and admin operation
 * @author Patson Luk
 *
 */
public class SolrCacheInstrumentation extends ClassInstrumentation {
    public static final String LAYER_NAME = "solr";
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        CtMethod getMethod = cc.getMethod("get", "(Ljava/lang/Object;)Ljava/lang/Object;");

        if (shouldModify(cc, getMethod)) {
            insertAfter(getMethod, getClass().getName() + ".recordResult($_, this);", true);
        }
        
        tagInterface(cc, SolrCache.class.getName());
        return true;
    }

    

    public static void recordResult(Object result, Object solrCacheObj) {
        Event infoEvent = Context.createEvent();
        
        infoEvent.addInfo("Label", "info",
                      "CacheHit", result != null);
        if (solrCacheObj instanceof SolrCache) {
            SolrCache solrCache = (SolrCache)solrCacheObj;
            if (solrCache.name() != null) {
                infoEvent.addInfo("CacheName", solrCache.name());
            }
            if (solrCache.getDescription() != null) {
                infoEvent.addInfo("CacheDescription", solrCache.getDescription());
            }
        }

        infoEvent.report();
    }
    
    public interface SolrCache {
        String name();
        String getDescription();
    }
}