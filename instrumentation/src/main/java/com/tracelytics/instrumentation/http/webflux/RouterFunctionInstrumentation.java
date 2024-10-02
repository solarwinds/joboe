package com.tracelytics.instrumentation.http.webflux;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.tracelytics.joboe.span.impl.Span;

/**
 * This class instruments the class RouterFunctions$DefaultRouterFunction of the WebFlux library.
 *
 * This captures the "Predicate" KV for the router function. For example: `((GET && /hello) && Accept: text/plain)`
 */
public class RouterFunctionInstrumentation extends ClassInstrumentation {
    public static final String CLASS_NAME = RouterFunctionInstrumentation.class.getName();
    
    private static List<MethodMatcher<Object>> methodMatchers = Collections.singletonList(
            new MethodMatcher<Object>("route", new String[]{"org.springframework.web.reactive.function.server.ServerRequest"}, "reactor.core.publisher.Mono"));
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (CtMethod handleMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            modifyHandle(handleMethod);
        }
        return true;
    }
    
    private void modifyHandle(CtMethod method) throws CannotCompileException {
        insertAfter(method,
                "$_ = $_.doOnSuccessOrError(" + CLASS_NAME + ".RouteConsumer.newInstance($0.toString(), $1.attributes()));"
                , true, false);
    }
    
    public static class RouteConsumer<T> implements java.util.function.BiConsumer<T, Throwable> {
        private String predicate;
        private Object currSpan;
        
        private RouteConsumer(String predicateString, Object span) {
            predicate = predicateString;
            currSpan = span;
        }
        
        public static RouteConsumer newInstance(String predicateString, Map<String, Object> attributes) {
            Span span = (Span) attributes.get(DispatcherHandlerInstrumentation.SPAN_ATTRIBUTE);
            return new RouteConsumer(predicateString, span);
        }
        
        @Override
        public void accept(T t, Throwable throwable) {
            if (currSpan != null) {
                Span span = (Span) currSpan;
    
                if (t != null) {
                    // It grabs something like `((GET && /hello) && Accept: text/plain)` from the predicate.
                    span.setTag("Predicate", predicate.replaceFirst("\\s*->.*$", ""));
                }
            }
        }
    }
}
