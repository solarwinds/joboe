package com.tracelytics.instrumentation.http.ws;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.instrumentation.http.ServletInstrumentation;

/**
 * Instrumentation on CXF SOAP ClientCallback. This is the exit point of asynchronous calls from client via
 * invoke(ClientCallback, ...)
 * 
 * @author Patson Luk
 *
 */
public class CxfClientCallbackInstrumentation extends BaseWsClientCallbackInstrumentation {

    private static String LAYER_NAME = "soap_client_cxf";
    private static String CLASS_NAME = CxfClientCallbackInstrumentation.class.getName();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtMethod handleResponseMethod = cc.getMethod("handleResponse", "(Ljava/util/Map;[Ljava/lang/Object;)V");
        CtMethod handleExceptionMethod = cc.getMethod("handleException", "(Ljava/util/Map;Ljava/lang/Throwable;)V");
        
        if (shouldModify(cc, handleResponseMethod)) {
            //do not do context check, as this relies on the x-trace id from the incoming response
            insertAfter(handleResponseMethod, CLASS_NAME + ".layerExit(this, ($1 != null && $1.containsKey(org.apache.cxf.message.Message.PROTOCOL_HEADERS)) ? (java.util.Map)$1.get(org.apache.cxf.message.Message.PROTOCOL_HEADERS) : null) ;", true, false);
        }
        
        if (shouldModify(cc, handleExceptionMethod)) {
          //do not do context check, as this relies on the x-trace id from the incoming response
            insertAfter(handleExceptionMethod, CLASS_NAME + ".layerExit(this, ($1 != null && $1.containsKey(org.apache.cxf.message.Message.PROTOCOL_HEADERS)) ? (java.util.Map)$1.get(org.apache.cxf.message.Message.PROTOCOL_HEADERS) : null) ;", true, false);
        }
        
        cc.addField(CtField.make("private String tvLayer;", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetLayer() { return tvLayer; }", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetLayer(String layer) { tvLayer = layer; }", cc));
        tagInterface(cc, CxfClientCallback.class.getName());
        
        addTvContextObjectAware(cc);
        
        return true;
    }
    
    public static void layerExit(Object callback, Map<String, Object> headers) {
        String responseXTraceId = null;
        if (headers != null) {
            for (Entry<String, Object> headerEntry : headers.entrySet()) {
                if (ServletInstrumentation.XTRACE_HEADER.equalsIgnoreCase(headerEntry.getKey())) {
                    List xTraceIdArray = (List) headerEntry.getValue();
                    if (xTraceIdArray != null && !xTraceIdArray.isEmpty()) {
                        responseXTraceId = (String)xTraceIdArray.get(0);
                    }
                    break; //found a match, leaving the loop
                }
            }
        }
        
        if (!(callback instanceof TvContextObjectAware)) {
            logger.warn("CXF ClientCallback is not wrapped by " + TvContextObjectAware.class.getName() + "! Unexpected.");
            return;
        }
        
        if (((TvContextObjectAware) callback).getTvContext() != null) { //only proceed if it has been traced
            String layerName = ((CxfClientCallback)callback).tvGetLayer();
            if (layerName == null) {
                logger.warn("Cannot recognize the CXF client callback, default to SOAP");
                layerName = LAYER_NAME;
            }
            layerExitAsync(layerName, callback, responseXTraceId);
        }
    }
}