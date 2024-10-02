package com.tracelytics.instrumentation.http.webflow;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;

/**
 * Instruments on <code>org.springframework.webflow.executor.mvc.FlowController</code>, entry point of Webflow 1 framework. Tracks the
 * entry and exit of the handle method and treats them as the start and end point of the webflow layer 
 *
 * @author Patson Luk
 *
 */
public class FlowControllerInstrumentation extends FlowEntryPointInstrumentation {
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        // Find "handleRequestInternal" method
        try {
            CtMethod method = cc.getMethod("handleRequestInternal", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/springframework/web/servlet/ModelAndView;");
            if (method.getDeclaringClass() == cc) {
                modifyHandleMethod(method);
            }
        } catch(NotFoundException ex) {
            logger.debug("Unable to find handle method", ex);
        }
        
        return true;
      
    }
}



