package com.tracelytics.instrumentation.http.ws;

import java.lang.reflect.Method;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.util.HttpUtils;


/**
 * Instrumentation for CXF Proxy based client. Instrumentation is done on org.apache.cxf.jaxrs.client.ClientProxyImpl's invoke() method
 * @author Patson Luk
 *
 */
public class CxfRsProxyClientInstrumentation extends BaseWsClientInstrumentation {
    
    private static String LAYER_NAME = "rest_client_cxf";

    private static String CLASS_NAME = CxfRsProxyClientInstrumentation.class.getName();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        
        CtMethod invokeMethod = cc.getMethod("invoke", "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;");
        if (shouldModify(cc, invokeMethod)) {
            insertBefore(invokeMethod, CLASS_NAME + ".beforeInvoke($2);");
            
            addErrorReporting(invokeMethod, "java.lang.Exception", LAYER_NAME, classPool);
                        
            insertAfter(invokeMethod, CLASS_NAME + ".afterInvoke($2, this);"
                                     , true);
        }
        
        return true;
    }

    public static void beforeInvoke(Method method) {
        if (isRemoteInvocation(method)) {
            Event event = Context.createEvent();
    
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry");
            
            event.report();
        }
    }
    
    public static void afterInvoke(Method method, Object clientObject) {
        if (isRemoteInvocation(method)) { 
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
            
            if (clientObject instanceof CxfRsClient) {
                CxfRsClient client = (CxfRsClient) clientObject;
                if (client.getUri() != null) {
                    event.addInfo("RemoteController", "REST",
                                  "IsService", true,
                                  "Spec", "rsc",
                                  "RemoteURL", hideUrlQuery ? HttpUtils.trimQueryParameters(client.getUri()) : client.getUri());
                }
                if (client.getHttpMethod() != null) {
                    event.addInfo("HTTPMethod", client.getHttpMethod());
                }
                if (client.getXTraceId() != null) {
                    event.addEdge(client.getXTraceId());
                }
            } else {
                logger.warn("Expect parameter as instance of [" + CxfRsClient.class.getName() + "] but it is not");
            }
            
            event.report();
        }
    }

    /**
     * Logic referenced from ClientProxyImpl.invoke
     * @param method
     * @return whether the method invocation is a remote one
     */
    private static boolean isRemoteInvocation(Method method) {
        if (method != null) {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass != null) {
                if (declaringClass.getName().equals("org.apache.cxf.jaxrs.client.Client") || 
                    declaringClass.getName().equals("org.apache.cxf.jaxrs.client.InvocationHandlerAware") || 
                    Object.class == declaringClass) {
                    return false;
                }
            }
        }
            
        return true;
    }
    
}