package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Instruments WebLogic's <code>weblogic.servlet.internal.WebAppServletContext</code> which handles exception and sets status code (usually 500) accordingly.
 * 
 * We need this extra instrumentation as the exception triggered status code is not set at HttpServlet level hence using HttpServlet instrumentation alone might not capture the erroneous status code properly.
 * 
 * Instead, we need to capture the method call earlier in the stack from <code>weblogic.servlet.internal.WebAppServletContext</code> which encloses the exception and status code handling.
 * 
 * @author pluk
 *
 */
public class WebLogicServletContextInstrumentation extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtMethod handlerRequestMethod = cc.getMethod("execute", "(Lweblogic/servlet/internal/ServletRequestImpl;Lweblogic/servlet/internal/ServletResponseImpl;)V");

        if (shouldModify(cc, handlerRequestMethod)) {
            insertBefore(handlerRequestMethod, SERVLET_CLASS_NAME + ".layerEntry(this, (Object)$1,(Object)$2);", false);
            addErrorReporting(handlerRequestMethod, Throwable.class.getName(), layerName, classPool);
            insertAfter(handlerRequestMethod, SERVLET_CLASS_NAME + ".layerExit(this, (Object)$1,(Object)$2);", true, false);
        }

        return true;

    }

    static public final String SERVLET_CLASS_NAME = ServletInstrumentation.class.getName();
}
