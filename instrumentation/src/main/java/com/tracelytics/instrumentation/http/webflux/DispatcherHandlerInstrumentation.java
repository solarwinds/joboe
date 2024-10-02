package com.tracelytics.instrumentation.http.webflux;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.span.impl.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The instrumentation of the class DispatcherHandler of the WebFlux library. This instrumentation class creates a new span for the WebFlux layer
 * and registers a consumer to the result of the handler to terminate the span. The span is expected to be layered under the netty/servlet root span.
 */
public class DispatcherHandlerInstrumentation extends ClassInstrumentation {
    public static final String CLASS_NAME = DispatcherHandlerInstrumentation.class.getName();
    public static final String SPAN_NAME = "spring-webflux";
    public static final String SPAN_ATTRIBUTE = Span.class.getName();
    
    private static List<MethodMatcher<Object>> methodMatchers = Collections.singletonList(
            new MethodMatcher<Object>("handle", new String[]{"org.springframework.web.server.ServerWebExchange"}, "reactor.core.publisher.Mono"));
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (CtMethod handleMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            modifyHandle(handleMethod);
        }
        return true;
    }
    
    private void modifyHandle(CtMethod method) throws CannotCompileException {
        insertBefore(method, "java.util.Map headers = $1.getRequest().getHeaders();"
            + "com.tracelytics.joboe.span.impl.Span span = " + CLASS_NAME + ".handleEntry(headers);"
            + "if (span != null) {"    
            + "$1.getAttributes().put(\"" + SPAN_ATTRIBUTE + "\",span);"
            + "}"    
                , false);
    
        insertAfter(method,
                "String patternKey = org.springframework.web.reactive.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;" 
                + "$_ = $_.doOnSuccess(" + CLASS_NAME + ".RouteConsumer.newInstance($1.getAttributes(), patternKey));"
                + "$_ = $_.doOnError(" + CLASS_NAME + ".RouteConsumer.newInstance($1.getAttributes(), patternKey));"
                , true, false);
        insertAfter(method,
                CLASS_NAME + ".handleExit();"
                , true, false);
    }
    
    public static Span handleEntry(Map headers) {
        List<String> spanIdList = (List<String>) headers.get(X_SPAN_KEY.toLowerCase());
        if (spanIdList == null || spanIdList.size() == 0) {
            return null;
        }

        String spanId = spanIdList.get(0);
        
        Span span = null;
        if (spanId != null) {
            Tracer.SpanBuilder builder = buildTraceEventSpan(SPAN_NAME);
            Span baseSpan = SpanDictionary.getSpan(Long.parseLong(spanId));
            if (baseSpan != null) {
                builder = builder.asChildOf(baseSpan);
            } else {
                logger.warn("Cannot continue trace on webflux dispatcher asn span with id " + spanId + " is not found");
                return null;
            }
            Scope scope = builder.startActive(false);
    
            span = scope.span();
            span.setSpanPropertyValue(Span.SpanProperty.IS_ASYNC, true);
        }
        return span;
    }
    
    public static void handleExit() {
        Scope scope = ScopeManager.INSTANCE.active();
        if (scope != null) {
            scope.close();
        }   
    }
    
    public static class RouteConsumer<T> implements java.util.function.Consumer<T> {
        private Map<String, Object> attributes;
        private final String patternKey;
        
        private RouteConsumer(Map<String, Object> attrs, String key) {
            attributes = attrs;
            patternKey = key;
        }
    
        public static RouteConsumer newInstance(Map<String, Object> attributes, String key) {
            return new RouteConsumer(attributes, key);
        }
        
        @Override
        public void accept(T t) {
            Object spanObj = attributes.get(DispatcherHandlerInstrumentation.SPAN_ATTRIBUTE);
            if (spanObj instanceof Span) {
                Span currSpan = (Span) spanObj;
                    Object pattern = attributes.get(patternKey);
                    if (pattern != null) {
                        currSpan.setTracePropertyValue(Span.TraceProperty.TRANSACTION_NAME, pattern.toString());
                    }
          
                currSpan.finish();
            }
        }
    }
}
