package com.tracelytics.instrumentation.http;

import java.lang.reflect.Method;
import java.util.Collections;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;


/**
 * Instruments Spring 3.2+ RequestMappingHandlerAdapter. This captures the annotated method used to handle the request
 *
 */
public class SpringRequestMappingHandlerAdapterInstrumentation extends ClassInstrumentation {
    public static final String CLASS_NAME = SpringRequestMappingHandlerAdapterInstrumentation.class.getName();
    
    private static final MethodMatcher<Object> methodMatcher = new MethodMatcher<Object>("handleInternal", new String[] {"javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse", "org.springframework.web.method.HandlerMethod"}, "org.springframework.web.servlet.ModelAndView"); 
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        
        for (CtMethod method : findMatchingMethods(cc, Collections.singletonList(methodMatcher)).keySet()) {
            insertBefore(method, 
                         "if ($3 != null) {" +
                              CLASS_NAME + ".reportHandlerMethod($3.getBeanType(), $3.getMethod());" +
                         "}", false);
        }
        
        return true;
    }


    /**
     * Logs the handler method call (controller/action)
     *    controller: class with the handler method
     *    action: the method in the handler class
     *
     * @param method  method being called in the handler class
     * @param handler handler/controller object
     */
    public static void reportHandlerMethod(Class<?> beanClass, Method method) {
        if (Context.getMetadata().isSampled()) {
            Event info = Context.createEvent();
            
            info.addInfo("Layer", LAYER_NAME,
                         "Label", "info");
            
            if (beanClass != null) {
                info.addInfo("Controller", beanClass.getName());
            }
            if (method != null) {
                info.addInfo("Action", method.getName());
            }
            
            info.report();
        }
        
        Span span = ScopeManager.INSTANCE.activeSpan();
        if (span != null) {
            if (beanClass != null) {
                span.setTracePropertyValue(TraceProperty.CONTROLLER, beanClass.getName());
            }
            if (method != null) {
                span.setTracePropertyValue(TraceProperty.ACTION, method.getName());
            }
        }

        
    }

    static String LAYER_NAME = "spring";
}
