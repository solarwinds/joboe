package com.tracelytics.instrumentation.http.webflow;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;

/**
 * Instruments on <code>org.springframework.webflow.mvc.servlet.FlowHandlerAdapter</code>, entry point of Webflow 2 framework. Tracks the
 * entry and exit of the handle method and treats them as the start and end point of the webflow layer
 *
 * @author Patson Luk
 *
 */
public class FlowHandlerAdapterInstrumentation extends FlowEntryPointInstrumentation {
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        // Find "handle":
        try {
            CtMethod method = cc.getMethod("handle", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Lorg/springframework/web/servlet/ModelAndView;");
            if (method.getDeclaringClass() == cc) {
                modifyHandleMethod(method);
            }
        } catch(NotFoundException ex) {
            logger.debug("Unable to find handle method", ex);
        }
        
        return true;
      
    }
}



