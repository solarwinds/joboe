package com.tracelytics.instrumentation.http.webflux;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;

/**
 * The instrumentation of interface HandlerAdapter of the WebFlux library.
 *
 * This extracts the Controller/Action properites
 */
public class HandlerAdapterInstrumentation extends ClassInstrumentation {
    public static final String CLASS_NAME = HandlerAdapterInstrumentation.class.getName();
    
    private static List<MethodMatcher<Object>> methodMatchers = Collections.singletonList(
            new MethodMatcher<Object>("handle", new String[]{"org.springframework.web.server.ServerWebExchange", "java.lang.Object"}, "reactor.core.publisher.Mono"));
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (CtMethod handleMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            modifyHandle(handleMethod);
        }
        return true;
    }
    
    private void modifyHandle(CtMethod method) throws CannotCompileException {
        insertBefore(method, "Object handler = $2;" 
                        + "String operationName = null;" 
                        + "java.lang.reflect.Method method = null;" 
                        + "Class clazz = null;" 
                        + "if (handler instanceof org.springframework.web.method.HandlerMethod) {" 
                        +     "org.springframework.web.method.HandlerMethod handlerMethod = (org.springframework.web.method.HandlerMethod) handler;" 
                        +     "method = handlerMethod.getMethod(); " 
                        + "} else {" 
                        +     "clazz = handler.getClass();" 
                        + "}"
                        + CLASS_NAME + ".handleEntry(method, clazz, $1.getAttributes());"
                , false);
    }
    
    public static void handleEntry(Method method, Class clazz, Map<String, Object> attrs) {
        if (attrs == null) {
            return;
        }

        Object spanObj = attrs.get(DispatcherHandlerInstrumentation.SPAN_ATTRIBUTE);
        if (!(spanObj instanceof Span)) {
            return;
        }

        Span span = (Span) spanObj;
        if (!span.getOperationName().equals(DispatcherHandlerInstrumentation.SPAN_NAME)) {
            return;
        }
        
        if (method != null) {
            String methodDeclaringClass = method.getDeclaringClass().getName();
            String methodName = method.getName();
            span.setTracePropertyValue(Span.TraceProperty.CONTROLLER, methodDeclaringClass);
            span.setTag("Controller", methodDeclaringClass);
            span.setTracePropertyValue(Span.TraceProperty.ACTION, methodName);
            span.setTag("Action", methodName);
        } else if (clazz != null) {
            String className = clazz.getName();
            final int lambdaIdx = className.indexOf("$$Lambda$");
    
            String controllerName = null;
            if (lambdaIdx > -1) {
                controllerName = className.substring(0, lambdaIdx) + ".lambda";
            } else {
                controllerName = className;
            }
            span.setTracePropertyValue(Span.TraceProperty.CONTROLLER, controllerName);
            span.setTag("Controller", controllerName);
        } else {
            logger.debug("No handler method or class find for HandlerAdapter.");
        }
        
    }
}
