package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Instruments IBM WebSphere's <code>com.ibm.wsspi.webcontainer.RequestProcessor</code> which handles exception and sets status code (usually 500) accordingly.
 * 
 * We need this extra instrumentation as the exception triggered status code is not set at HttpServlet level hence using HttpServlet instrumentation alone might not capture the erroneous status code properly.
 * 
 * Instead, we need to capture the method call earlier in the stack from <code>CacheServletWrapper</code> (implements <code>RequestProcessor</code>) which encloses the exception and status code handling.
 * 
 * @author pluk
 *
 */
public class IbmRequestProcessorInstrumentation extends ClassInstrumentation {

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtMethod handlerRequestMethod = cc.getMethod("handleRequest", "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V");
        
        if (shouldModify(cc, handlerRequestMethod)) {
            insertBefore(handlerRequestMethod, SERVLET_CLASS_NAME + ".layerEntry(this, (Object)$1,(Object)$2);", false);
            addErrorReporting(handlerRequestMethod, Throwable.class.getName(), layerName, classPool);
            insertAfter(handlerRequestMethod, SERVLET_CLASS_NAME + ".layerExit(this, (Object)$1,(Object)$2);", true, false);
        }
        
        return true;
   
    }

    static public final String SERVLET_CLASS_NAME = ServletInstrumentation.class.getName();
}
