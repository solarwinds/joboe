package com.tracelytics.instrumentation.http.ws;

import java.util.List;
import java.util.Map;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Instrumentation on Glasshfish Jersey ResponseCallback. This is the exit point of asynchronous calls from client via
 * target.request().async().get(SampleResultContainer.class); which target is a Jax-RS WebTarget
 * 
 * @author Patson Luk
 *
 */
public class GlassfishJerseyResponseCallbackInstrumentation extends BaseWsClientInstrumentation {

    private static String LAYER_NAME = "rest_client_jersey";
    private static String CLASS_NAME = GlassfishJerseyResponseCallbackInstrumentation.class.getName();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtMethod completedMethod = cc.getMethod("completed", "(Lorg/glassfish/jersey/client/ClientResponse;Lorg/glassfish/jersey/process/internal/RequestScope;)V");
        
        if (shouldModify(cc, completedMethod)) {
            insertAfter(completedMethod, CLASS_NAME + ".layerExit(this, $1 != null ? $1.getHeaders() : null);", true, false);
        }
        
        addTvContextObjectAware(cc);
        
        //TODO not catching the failed method for now as the complete method might call the failed handling and cause double instrumentation
//        CtMethod failedMethod;
//        try {
//            failedMethod = cc.getMethod("failed", "(Ljavax/ws/rs/ProcessingException;)V");
//        } catch (NotFoundException e) {
//            logger.debug("Cannot find failed(Ljavax/ws/rs/ProcessingException;)V, trying older signature");
//            failedMethod = cc.getMethod("failed", "(Ljavax/ws/rs/client/ClientException;)V");
//        }
//        if (shouldModify(cc, failedMethod)) {
//            failedMethod.insertAfter(CLASS_NAME + ".layerExit(this, null);", true);
//        }
            
        return true;
    }

  
    public static void layerExit(Object responseCallback, Map headers) {
        //Attempt to lookup the metadata of the entry event that spawns this responseCallback object
        if (!(responseCallback instanceof TvContextObjectAware)) {
            logger.warn("Glassfish jersey response is not tagged as " + TvContextObjectAware.class.getName() + "properly");
            return;
        }
        Metadata previousContext = ((TvContextObjectAware)responseCallback).getTvContext();
        
        if (previousContext != null) {
            Context.setMetadata(previousContext);
        } else {
            logger.debug("Cannot retrieve previous edge for asynchronous Glassfish Jersey client calls, it was not instrumented");
            return;
        }
                
        String responseXTraceId = null;
        if (headers != null) {
            List xTraceIdArray = (List) headers.get(ServletInstrumentation.XTRACE_HEADER);
            if (xTraceIdArray != null && !xTraceIdArray.isEmpty()) {
                responseXTraceId = (String)xTraceIdArray.get(0);
            }
        }

        layerExitRest(LAYER_NAME, responseXTraceId, true); //it should be an asynchronous event as it is using the Callback
    }
}