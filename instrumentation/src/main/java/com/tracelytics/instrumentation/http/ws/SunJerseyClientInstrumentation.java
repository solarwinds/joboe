package com.tracelytics.instrumentation.http.ws;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.http.ServletInstrumentation;

/**
 * Instrumentation of Sun Jersey (1.x) Rest client
 * @author Patson Luk
 *
 */
public class SunJerseyClientInstrumentation extends BaseWsClientInstrumentation {

    private static String LAYER_NAME = "rest_client_jersey";
    private static String CLASS_NAME = SunJerseyClientInstrumentation.class.getName();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtMethod handleMethod = cc.getMethod("handle", "(Lcom/sun/jersey/api/client/ClientRequest;)Lcom/sun/jersey/api/client/ClientResponse;");
        
        if (shouldModify(cc, handleMethod)) {
            insertBefore(handleMethod, CLASS_NAME + ".beforeHandle($1.getURI(), $1.getMethod());");
                                      
            insertAfter(handleMethod, CLASS_NAME + ".afterHandle($_ != null ? $_.getHeaders() : null, this.getClass().getName(), $1);", true);
        }

        return true;
    }

  
    public static void beforeHandle(URI resourceURI, String httpMethod) {
        layerEntryRest(httpMethod, resourceURI != null ?  resourceURI.toString() : null, LAYER_NAME);
    }

    public static void afterHandle(Map headers, String clientClassName, Object clientRequest) {
        String xTraceId = null; 
        
        //do not attempt to handle the header if this is an Apache Http client, as it should already be handled by the client's instrumentation
        boolean handleHeader = !"com.sun.jersey.client.apache.ApacheHttpClient".equals(clientClassName);
        
        if (handleHeader && headers != null) {
            List xTraceIdArray = (List) headers.get(ServletInstrumentation.XTRACE_HEADER);
            if (xTraceIdArray != null && !xTraceIdArray.isEmpty()) {
                xTraceId = (String)xTraceIdArray.get(0);
            }
        }
        
        boolean isAsync = clientRequest instanceof SunJerseyClientRequest && ((SunJerseyClientRequest)clientRequest).isAsync();
        
        layerExitRest(LAYER_NAME, xTraceId, isAsync);
    }
}