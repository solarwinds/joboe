package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;

/**
 * Instruments Spring Controllers - anything that extends org.springframework.web.servlet.mvc.Controller
 * with special handling for multiaction controllers
 */
public class SpringControllerInstrumentation extends ClassInstrumentation {
    public static final String CLASS_NAME = SpringControllerInstrumentation.class.getName();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {

        // Determine if class is a MultiAction controller or a simple controller
        // XXX: other controller types???
        try {
            CtClass multiAction = classPool.getCtClass("org.springframework.web.servlet.mvc.multiaction.MultiActionController");
            if (cc.equals(multiAction)) {
                instrumentMultiActionController(cc);
            } else {
                instrumentSimpleController(cc);
            }
        } catch (NotFoundException e) {
            logger.debug("Cannot load class org.springframework.web.servlet.mvc.multiaction.MultiActionController, likely spring 5+ version");
            instrumentSimpleController(cc);
        }
        

        return true;
    }

    private void instrumentSimpleController(CtClass cc)
            throws NotFoundException {

        try {
            // Instrument the generic "handleRequest" that is required by all controllers
            CtMethod handleMethod = cc.getMethod("handleRequest", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/springframework/web/servlet/ModelAndView;");
            if (handleMethod.getDeclaringClass() == cc) {
                insertBefore(handleMethod, CLASS_NAME +".doHandleRequestInfo(this);", false);
            }
            
        } catch(CannotCompileException ex) {
            logger.debug("Unable to find handleRequest in " + cc.getName());
        }
    }

    private void instrumentMultiActionController(CtClass cc)
        throws NotFoundException {
        try {

            // Need to tag interface so during handleRequestInfo we skip this one... since MultiActionControllers are
            // also regular controllers, we'd wind up generating 2 info calls.
            CtClass multiAction = classPool.getCtClass("com.tracelytics.instrumentation.http.SpringMultiActionController");
            cc.addInterface(multiAction);

            CtMethod invokeMethod = cc.getMethod("invokeNamedMethod", "(Ljava/lang/String;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/springframework/web/servlet/ModelAndView;");
            if (invokeMethod.getDeclaringClass() == cc) {
                insertBefore(invokeMethod, CLASS_NAME +".doInvokeNamedMethodInfo(this, $1);", false);
            }

        } catch(CannotCompileException ex) {
            logger.debug("Unable to find invokeNamedMethod in " + cc.getName());
        }
    }

    /**
     * Logs the handleRequest method call, for controllers that only implement a single action.
     *    controller: class with the handler method
     *    action: "handleRequest" - method name called on the controller
     *
     * @param handler handler/controller object
     */
    public static void doHandleRequestInfo(Object handler) {
        if (handler instanceof SpringMultiActionController) {
            // MultiAction controllers are handled in doHandleInvokeNamedMethod
            return;
        }

        if (Context.getMetadata().isSampled()) {
            Event info = Context.createEvent();
            if (info != null) {
                info.addInfo("Layer", SpringHandlerAdapterInstrumentation.LAYER_NAME,
                             "Label", "info",
                             "Controller", handler.getClass().getName(),
                             "Action", "handleRequest"
                            );
                info.report();
            }
        }
        
        Span span = ScopeManager.INSTANCE.activeSpan();
        if (span != null) {
            span.setTracePropertyValue(TraceProperty.CONTROLLER, handler.getClass().getName());
            span.setTracePropertyValue(TraceProperty.ACTION, "handleRequest");
        }
    }

    /**
     * Logs the invokeNamedMethod call, for controllers that implement multiple actions.
     *    controller: class with the handler method
     *    action: "method name called on the controller
     *
     * @param handler handler/controller object
     * @param method method name invoked on controller
     */
    public static void doInvokeNamedMethodInfo(Object handler, String method) {
        if (Context.getMetadata().isSampled()) {
            Event info = Context.createEvent();
            if (info != null) {
                info.addInfo("Layer", SpringHandlerAdapterInstrumentation.LAYER_NAME,
                             "Label", "info",
                             "Controller", handler.getClass().getName(),
                             "Action", method
                            );
                info.report();
            }
        }
        
        Span span = ScopeManager.INSTANCE.activeSpan();
        if (span != null) {
            span.setTracePropertyValue(TraceProperty.CONTROLLER, handler.getClass().getName());
            span.setTracePropertyValue(TraceProperty.ACTION, method);
        }
    }
}
