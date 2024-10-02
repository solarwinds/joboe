package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodSignature;
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

public class ResteasyClientInvocationInstrumentation extends BaseWsClientInstrumentation {

    private static String LAYER_NAME = "rest_client_resteasy";
    private static String CLASS_NAME = ResteasyClientInvocationInstrumentation.class.getName();
    
    private static final MethodSignature[] SUBMIT_METHOD_SIGS = new MethodSignature[] { new MethodSignature("submit", "()Ljava/util/concurrent/Future;"),
                                                                                        new MethodSignature("submit", "(Ljava/lang/Class;)Ljava/util/concurrent/Future;"),
                                                                                        new MethodSignature("submit", "(Ljavax/ws/rs/core/GenericType;)Ljava/util/concurrent/Future;"),
                                                                                        new MethodSignature("submit", "(Ljavax/ws/rs/client/InvocationCallback;)Ljava/util/concurrent/Future;") }; 
    private static final String IS_ASYNC_FIELD_NAME = "tvIsAsync";
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtMethod executeMethod = cc.getMethod("invoke", "()Ljavax/ws/rs/core/Response;");
        
        if (shouldModify(cc, executeMethod)) {
            insertBefore(executeMethod, CLASS_NAME + ".layerEntry();");
                                      
            insertAfter(executeMethod, CLASS_NAME + ".layerExit(this.getMethod(), this.getUri() != null ? this.getUri().toString() : null, this);", true);
        }
        
        cc.addField(CtField.make("private boolean " + IS_ASYNC_FIELD_NAME + " = false;", cc));        
        cc.addMethod(CtNewMethod.make("public void setAsync(boolean isAsync) { " + IS_ASYNC_FIELD_NAME + " = isAsync; }", cc));
        cc.addMethod(CtNewMethod.make("public boolean isAsync() { return " + IS_ASYNC_FIELD_NAME + "; }", cc));
        
        for (MethodSignature sig : SUBMIT_METHOD_SIGS) {
            try {
                CtMethod submitMethod = cc.getMethod(sig.getName(), sig.getSignature());
                if (shouldModify(cc, submitMethod)) {
                    insertBefore(submitMethod, CLASS_NAME + ".markAsync(this);");
                }
            } catch (NotFoundException e) { 
                logger.warn("Cannot locate method: " + sig + " in class " + className);
            }
        }
        
        tagInterface(cc, ResteasyClientInvocation.class.getName());

        return true;
    }

  
    public static void layerEntry() {
        Event event = Context.createEvent();
        
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry");
        
        event.report();
    }

    public static void layerExit(String httpMethod, String uri, Object clientInvocation) {
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
        
        if (clientInvocation instanceof ResteasyClientInvocation) {
            if (((ResteasyClientInvocation)clientInvocation).isAsync()) {
                event.setAsync();
            }
        } else {
            logger.warn("Expected JBoss Resteasy client invocation to be wrapped by " + ResteasyClientInvocation.class.getName());
        }
        
        event.report();
    }
    
    public static void markAsync(Object clientInvocation) {
        if (clientInvocation instanceof ResteasyClientInvocation) {
            ((ResteasyClientInvocation)clientInvocation).setAsync(true);
        } else {
            logger.warn("Expected JBoss Resteasy client invocation to be wrapped by " + ResteasyClientInvocation.class.getName());
        }
    }
}