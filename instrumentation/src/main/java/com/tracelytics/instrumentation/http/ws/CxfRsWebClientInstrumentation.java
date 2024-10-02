package com.tracelytics.instrumentation.http.ws;

import java.net.URI;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Instrumentation for CXF WebClient API client. Instrumentation is done on org.apache.cxf.jaxrs.client.AbstractClient doInvoke() and doInvokeAsyn() method.
 * 
 * For asynchronous handling, it is quite tricky as the starting point (invokeAsync) does not have a matching ending point in the same thread. The ending point can be traced
 * in JaxrsClientCallback, however it is hard to relate a specific callback to the starting point as there could be multiple of them. Since invokeAsync (entry event) would return
 * a JaxrsClientCallback (as java.util.concurrent.Future), we will keep it as the key in a weak map to pair it up with the exit event. 
 * 
 * @author Patson Luk
 *
 */
public class CxfRsWebClientInstrumentation extends BaseWsClientInstrumentation {

    static String LAYER_NAME = "rest_client_cxf";

    private static String CLASS_NAME = CxfRsWebClientInstrumentation.class.getName();
    
    private static final String CONTEXT_FIELD_NAME = "tvContext";

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        cc.addField(CtField.make("private " + Metadata.class.getName() + " " + CONTEXT_FIELD_NAME + ";", cc));        
        
        CtMethod doInvokeMethod;
        try {
            doInvokeMethod = cc.getMethod("doInvoke", "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/reflect/Type;Ljava/lang/Class;Ljava/lang/reflect/Type;)Ljavax/ws/rs/core/Response;");
        } catch (NotFoundException e) {
            logger.debug("Could not find the doInoke() method, try the older signature");
            doInvokeMethod = cc.getMethod("doInvoke", "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Class;)Ljavax/ws/rs/core/Response;");
        }
        
        if (shouldModify(cc, doInvokeMethod)) {
            insertBefore(doInvokeMethod, CLASS_NAME + ".beforeInvoke($1, getCurrentURI());");
            addErrorReporting(doInvokeMethod, "java.lang.Exception", LAYER_NAME, classPool);
            insertAfter(doInvokeMethod, CLASS_NAME + ".afterInvoke(this);", true);
        }

        try {
            CtMethod doInvokeAsyncMethod = cc.getMethod("doInvokeAsync", "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/reflect/Type;Ljava/lang/Class;Ljava/lang/reflect/Type;Ljavax/ws/rs/client/InvocationCallback;)Ljava/util/concurrent/Future;");
    
            if (shouldModify(cc, doInvokeAsyncMethod)) {
                insertBefore(doInvokeAsyncMethod, CONTEXT_FIELD_NAME + " = " + CLASS_NAME + ".beforeInvokeAsync($1, getCurrentURI());" +
            //since CXF uses SocketChannel (instead of URLConnection) when making Asynchronous calls, we cannot rely on HttpURLConnectionPatcher to insert X-trace id
                                                  "if (" + CONTEXT_FIELD_NAME + " != null) { replaceHeader(\"" + ServletInstrumentation.XTRACE_HEADER + "\", " + CONTEXT_FIELD_NAME + ".toHexString()); }", false); 
                                                
                String callbackClassName = CxfRsResponseCallback.class.getName();
                insertAfter(doInvokeAsyncMethod, CLASS_NAME + ".storeContext($_ instanceof " + callbackClassName + " ? ((" + callbackClassName + ")$_).getClientCallback() : null, " + CONTEXT_FIELD_NAME + ");" + //tag the x-trace id to the future object (JaxrsResponseCallback)
                                                 CONTEXT_FIELD_NAME + " = null;" +
                                                 CLASS_NAME + ".setLayerToCallback($_ instanceof " + callbackClassName + " ? ((" + callbackClassName + ")$_).getClientCallback() : null);" , true);
            }
    
//            CtMethod handleAsyncResponseMethod = cc.getMethod("handleAsyncResponse", "(Lorg/apache/cxf/message/Message;)V");
//            if (shouldModify(cc, handleAsyncResponseMethod)) {
//                addErrorReporting(handleAsyncResponseMethod, "java.lang.Exception", LAYER_NAME, classPool);
//                //map the exit event with the exit event via the JaxrsClientCallback (as java.util.concurrent.Future) in the Exchange. This is the only object found shared by the entry and exit event
//                insertAfter(handleAsyncResponseMethod, CLASS_NAME + ".afterHandleAsyncResponse($1.getExchange().get(org.apache.cxf.jaxrs.client.JaxrsClientCallback.class), this);", true, false);
//            }
        } catch (NotFoundException e) { //older version then
            logger.debug("Cannot locate the doInvokeAsync/handleAsyncResponse method of newer CXF version, Probably running on older CXF versions...");
        }

        return true;
    }

    public static void beforeInvoke(String httpMethod, URI uri) {
        layerEntryRest(httpMethod, uri != null ? uri.toString() : null, LAYER_NAME, false);
    }
    
    public static void afterInvoke(Object clientObject) {
        if (clientObject instanceof CxfRsClient) {
            CxfRsClient client = (CxfRsClient) clientObject;
            layerExitRest(LAYER_NAME, client.getXTraceId(), false);
        } else {
            logger.warn("Expect parameter as instance of [" + CxfRsClient.class.getName() + "] but it is not");
        }
    }

    /**
     * 
     * @return the current context(Metadata) after the entry event is created
     */
    public static Metadata beforeInvokeAsync(String httpMethod, URI uri) {
        if (Context.getMetadata().isSampled()) {
            return layerEntryRest(httpMethod, uri != null ? uri.toString() : null, LAYER_NAME, true);
        } else if (Context.getMetadata().isValid()) { //not tracing, but should still return it for injection in request header 
            return Context.getMetadata();
        } else {
            return null;
        }
    }
    
    public static void setLayerToCallback(Object callbackObject) {
        if (callbackObject instanceof CxfClientCallback) {
            ((CxfClientCallback)callbackObject).tvSetLayer(LAYER_NAME);
        } else {
            logger.warn("Expect callback as instance of [" + CxfClientCallback.class.getName() + "] but it is not");
        }
    }

    /**
     * Lookup the correct context by using the metadataMap, use the clientCallback as key for the lookup. 
     * 
     * If the previous context is found, set the current context with that metadata
     * 
     * @param metadataMap
     * @param clientCallback
     * @param clientObject
     */
    public static void afterHandleAsyncResponse(Object clientCallback, Object clientObject) {
        if (clientCallback instanceof TvContextObjectAware) {
            Metadata metadata = ((TvContextObjectAware)clientCallback).getTvContext();
            if (metadata != null) {
                //set the current context to the metadata
                Context.setMetadata(metadata);
            } else {
                logger.warn("Cannot find the previous context of CXF webclient asyn call exit event!");
            }
            
            String responseXTraceId = null;
            if (clientObject instanceof CxfRsClient) {
                CxfRsClient client = (CxfRsClient) clientObject;
                responseXTraceId = client.getXTraceId();
            } else {
                logger.warn("Expect parameter as instance of [" + CxfRsClient.class.getName() + "] but it is not");
            }
            
            
            layerExitRest(LAYER_NAME, responseXTraceId, true);
    
        } else {
            logger.warn("Callback is not tagged as " + TvContextObjectAware.class.getName()  + ". Failed to create exit event for CXF RS async handling");
        }
    }
}