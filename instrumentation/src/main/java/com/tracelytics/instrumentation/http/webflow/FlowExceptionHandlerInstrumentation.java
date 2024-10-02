package com.tracelytics.instrumentation.http.webflow;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Tracks exceptions handled by the Webflow framework via <code>org.springframework.webflow.engine.FlowExecutionExceptionHandler</code>
 * @author Patson Luk
 *
 */
public class FlowExceptionHandlerInstrumentation extends BaseWebflowInstrumentation {
    public static final String CLASS_NAME = FlowExceptionHandlerInstrumentation.class.getName();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        
        // Find "handle":
        try {
            CtMethod method = cc.getMethod("handle", "(Lorg/springframework/webflow/execution/FlowExecutionException;Lorg/springframework/webflow/engine/RequestControlContext;)V");
            if (method.getDeclaringClass().equals(cc)) {
                modifyHandleMethod(method);
            }
        } catch(NotFoundException ex) {
            logger.debug("Unable to find handle method", ex);
        }
        
        return true;
      
    }
    
    /**
     *   Modifies the handle method of the flow handler adapter
     * @param method
     * @throws CannotCompileException
     */
    protected void modifyHandleMethod(CtMethod method)
            throws CannotCompileException {

        insertBefore(method, CLASS_NAME + ".handleEntry($1);");
    }



    // These methods are called by the instrumented HttpServlet object
    public static void handleEntry(Exception flowException) {
        Event event = Context.createEvent();
        
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "info",
                      "Type", "Handle an Exception");
        
        event.addInfo("ErrorClass", flowException.getClass().getName());
        event.addInfo("ErrorMsg", flowException.getMessage() == null ? "<undefined>" : flowException.getMessage());
        
        ClassInstrumentation.addBackTrace(event, 1, Module.WEBFLOW);
        
        event.report();
    }
}



