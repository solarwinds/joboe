package com.tracelytics.instrumentation.solr;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instrumentation on the base class RequestHandlerBase of all Solr handlers. Solr applies handler to handle each query request and admin operation
 * @author Patson Luk
 *
 */
public class SolrHandlerInstrumentation extends ClassInstrumentation {
    private static final String LAYER_NAME = "solr-handler";
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        CtMethod handleRequestMethod;
        try { 
            //try Solr 3/4 signature
            handleRequestMethod = cc.getMethod("handleRequest", "(Lorg/apache/solr/request/SolrQueryRequest;Lorg/apache/solr/response/SolrQueryResponse;)V");
        } catch (NotFoundException e) {
            //try older signature
            handleRequestMethod = cc.getMethod("handleRequest", "(Lorg/apache/solr/request/SolrQueryRequest;Lorg/apache/solr/request/SolrQueryResponse;)V");
        }

        if (shouldModify(cc, handleRequestMethod)) {
            insertBefore(handleRequestMethod, getClass().getName() + ".profileEntry(this, $1 != null ? $1.getParamString() : null, $2);");
            insertAfter(handleRequestMethod, getClass().getName() + ".profileExit(this);", true);
            
            tagInterface(cc, SolrRequestHandler.class.getName());
            return true;
        } else {
            return false;
        }
                         

    }
    
    public static void profileEntry(Object handler, String paramString, Object Response) {
        
        
        if (handler instanceof SolrRequestHandler) {
            Event event = Context.createEvent();
            
            event.addInfo("Label", "entry",
                          "Layer", LAYER_NAME,
                          "HandlerClassName", handler.getClass().getName());
            
            if (((SolrRequestHandler) handler).getDescription() != null) {
                event.addInfo("HandlerDescription", ((SolrRequestHandler) handler).getDescription());
            }
            
            if (paramString != null) {
                event.addInfo("Param", paramString);
            }
            
            event.report();
        } else {
            logger.warn("Expect Solr handler to be tagged with interface [" + SolrRequestHandler.class.getName() + "], but it is not");
        }
    }
        
    
    
    public static void profileExit(Object handler) {
        if (handler instanceof SolrRequestHandler) {
            Event event = Context.createEvent();
            event.addInfo("Label", "exit",
                          "Layer", LAYER_NAME);
            
            event.report();
        } else {
            logger.warn("Expect Solr handler to be tagged with interface [" + SolrRequestHandler.class.getName() + "], but it is not");
        }
    }
   
}