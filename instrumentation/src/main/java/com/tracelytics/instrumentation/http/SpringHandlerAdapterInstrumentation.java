package com.tracelytics.instrumentation.http;

import java.lang.reflect.Method;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.expr.ExprEditor;
import com.tracelytics.ext.javassist.expr.MethodCall;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;


/**
 * Instruments Spring HandlerAdapters.  Handler Adapters is the code that calls into the controller.
 *
 */
public class SpringHandlerAdapterInstrumentation extends ClassInstrumentation {
    public static final String CLASS_NAME = SpringHandlerAdapterInstrumentation.class.getName();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {

        try {
            // This is for annotated spring controllers: http://static.springsource.org/spring/docs/3.0.0.M3/spring-framework-reference/html/ch16s11.html
            // We find the method that dispatches to controller and method.
            if (cc.getName().equals("org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter")) {
                CtMethod handleMethod = cc.getMethod("invokeHandlerMethod", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Lorg/springframework/web/servlet/ModelAndView;");
                if (handleMethod.getDeclaringClass() == cc) {
                    modifyInvokeHandlerMethod(handleMethod);
                }
            }

        } catch(CannotCompileException ex) {
            logger.debug("Unable to find handle method in " + cc.getName());
        }

        return true;
    }


    // Modify the invokeHandlerMethod call
    // See http://grepcode.com/file/repo1.maven.org/maven2/org.springframework/spring-webmvc/3.0.3.RELEASE/org/springframework/web/servlet/mvc/annotation/AnnotationMethodHandlerAdapter.java
    public void modifyInvokeHandlerMethod(CtMethod method) throws CannotCompileException {
        // Edit the method: we're not just doing a before/after : http://www.csg.is.titech.ac.jp/~chiba/javassist/tutorial/tutorial2.html
        method.instrument(new ExprEditor() {

            public void edit(MethodCall m) throws CannotCompileException {
                boolean addedInfo = false;
                // we're looking for: Object result = methodInvoker.invokeHandlerMethod(handlerMethod, handler, webRequest, implicitModel);
                if (m.getMethodName().equals("invokeHandlerMethod") && !addedInfo) {
                    insertBeforeMethodCall(m, CLASS_NAME + ".doInvokeHandlerMethodInfo($1, $2);", false);
                    addedInfo = true;
                }
            }
        });

    }

    /**
     * Logs the handler method call (controller/action)
     *    controller: class with the handler method
     *    action: the method in the handler class
     *
     * @param method  method being called in the handler class
     * @param handler handler/controller object
     */
    public static void doInvokeHandlerMethodInfo(Method method, Object handler) {
        if (Context.getMetadata().isSampled()) {
            Event info = Context.createEvent();
            if (info != null) {
                info.addInfo("Layer", LAYER_NAME,
                             "Label", "info",
                             "Controller", handler.getClass().getName(),
                             "Action", method.getName()
                            );
                info.report();
            }
        }
        
        Span span = ScopeManager.INSTANCE.activeSpan();
        if (span != null) {
            span.setTracePropertyValue(TraceProperty.CONTROLLER, handler.getClass().getName());
            span.setTracePropertyValue(TraceProperty.ACTION, method.getName());
        }
    }
            

    static String LAYER_NAME = "spring";
}
