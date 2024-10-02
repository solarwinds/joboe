package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Wrap the AsyncWebResource to set the Async flag in the SunJerseyClientRequest. The flag is to be used later on to determine whenever the extend is asynchronous
 * @author Patson Luk
 *
 */
public class SunJerseyAsyncWebResourceInstrumentation extends ClassInstrumentation {

    private static String CLASS_NAME = SunJerseyAsyncWebResourceInstrumentation.class.getName();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtMethod handleMethod = cc.getMethod("handle", "(Lcom/sun/jersey/api/client/ClientRequest;Lcom/sun/jersey/api/client/async/FutureListener;)Ljava/util/concurrent/Future;");
        
        if (shouldModify(cc, handleMethod)) {
            insertBefore(handleMethod, CLASS_NAME + ".setRequestAsyncFlag($1);", false);
        }
        return true;
    }

  
    public static void setRequestAsyncFlag(Object request) {
        if (request instanceof SunJerseyClientRequest) {
            ((SunJerseyClientRequest)request).setAsync(true);
        } else {
            logger.warn("Expected Sun Jersey Client request to be interface of [" + SunJerseyClientRequest.class.getName() + "], but it is not.");
        }
    }
}