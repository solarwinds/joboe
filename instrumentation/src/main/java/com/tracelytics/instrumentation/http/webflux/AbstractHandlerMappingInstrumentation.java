package com.tracelytics.instrumentation.http.webflux;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.span.impl.Span;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The instrumentation of the abstract class AbstractHandler. It tries to fetch the best match url pattern. If found, the pattern will be
 * used as the transaction name. For example, if the url pattern is `/name/{name}`, then the transaction name will be set to `/name/_name_`
 */
public class AbstractHandlerMappingInstrumentation extends ClassInstrumentation {
    public static final String CLASS_NAME = AbstractHandlerMappingInstrumentation.class.getName();
    
    private static List<MethodMatcher<Object>> methodMatchers = Collections.singletonList(
            new MethodMatcher<Object>("getHandler", 
                    new String[]{"org.springframework.web.server.ServerWebExchange"}, "reactor.core.publisher.Mono")
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (CtMethod handleMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            modifyHandle(handleMethod);
        }
        return true;
    }
    private void modifyHandle(CtMethod method) throws CannotCompileException {
        insertAfter(method,
                "String key = org.springframework.web.reactive.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;" 
                + "$_ = $_.doOnSuccess(" + CLASS_NAME + ".RouteConsumer.newInstance($1.getAttributes(), key));"
                + "$_ = $_.doOnError(" + CLASS_NAME + ".RouteConsumer.newInstance($1.getAttributes(), key));"
                , true, false);
    }
    
    public static class RouteConsumer<T> implements java.util.function.Consumer<T> {
        private Map attrs;
        private String key;
        
        private RouteConsumer(Map attrsMap, String k) {
            attrs = attrsMap;
            key = k;
        }
        
        public static RouteConsumer newInstance(Map<String, Object> attributes, String key) {
            return new RouteConsumer(attributes, key);
        }
        
        @Override
        public void accept(T t) {
            Object bestMatch = attrs.get(key);
            if (bestMatch != null) {
                String transactionName = bestMatch.toString();
                Span span = (Span) attrs.get(DispatcherHandlerInstrumentation.SPAN_ATTRIBUTE);
                if (span != null) {
                    span.setTracePropertyValue(Span.TraceProperty.TRANSACTION_NAME, transactionName);
                }
            }
        }
    }
}
