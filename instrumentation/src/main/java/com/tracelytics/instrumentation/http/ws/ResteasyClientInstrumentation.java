package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.util.HttpUtils;

/**
 * Instrumentation for JBoss RESTEasy client. There are two client implementations: 1. Manual ClientRequest API and 2. Proxy/ProxyFactory. Both of them
 * will eventually invoke the execute() method of org.jboss.resteasy.client.ClientRequest. Therefore, we will trace the start and end event surrounding that method.
 * 
 * JBoss RESTEasy makes use of Apache Http client therefore handling of x-trace id in Http response header is not necessary
 * 
 * @author Patson Luk
 *
 */

public class ResteasyClientInstrumentation extends BaseWsClientInstrumentation {

    private static String LAYER_NAME = "rest_client_resteasy";
    private static String CLASS_NAME = ResteasyClientInstrumentation.class.getName();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtMethod executeMethod = cc.getMethod("execute", "()Lorg/jboss/resteasy/client/ClientResponse;");
        
        if (shouldModify(cc, executeMethod)) {
            insertBefore(executeMethod, CLASS_NAME + ".layerEntry();");
                                      
            insertAfter(executeMethod, CLASS_NAME + ".layerExit(this.getHttpMethod(), this.getUri());", true);
        }

        return true;
    }

  
    public static void layerEntry() {
        Event event = Context.createEvent();
        
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry");
        
        event.report();
    }

    public static void layerExit(String httpMethod, String uri) {
        Event event = Context.createEvent();
        
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit");
        
        if (uri != null) {
            event.addInfo("IsService", true,
                          "RemoteController", "REST",
                          "Spec", "rsc",
                          "RemoteURL", hideUrlQuery ? HttpUtils.trimQueryParameters(uri) : uri);
        }
        
        if (httpMethod != null) {
            event.addInfo("HTTPMethod", httpMethod);
        }
        
        event.report();
    }
}