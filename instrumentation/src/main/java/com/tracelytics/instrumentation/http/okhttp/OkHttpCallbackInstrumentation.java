package com.tracelytics.instrumentation.http.okhttp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.SpanAware;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;

/**
 * Instruments `okhttp3.Callback` to create exit event for enqueue (asynchronous) operations.
 * 
 * The span is first created at the enqueue operation which is captured in {@link OkHttpCallInstrumentation}, and such a span will then be propagated to the callback and finishes when either the `onFailure` or `onResponse` method of the callback is invoked
 * 
 * Take note that we cannot tag the span to the callback instance as the instance can be reused for multiple calls. 
 * 
 * Instead we tag the span to the `Call` instance, which is available to the callback and unique for each enqueue operation
 * 
 * @author Patson
 *
 */

public class OkHttpCallbackInstrumentation extends ClassInstrumentation {
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
        new MethodMatcher<MethodType>("onFailure", new String[] { "okhttp3.Call", "java.io.IOException"}, "void", MethodType.ON_FAILURE),
        new MethodMatcher<MethodType>("onResponse", new String[] { "okhttp3.Call", "okhttp3.Response"}, "void", MethodType.ON_RESPONSE)
    );
    
    private enum MethodType {
        ON_FAILURE, ON_RESPONSE
    }
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
         
        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            MethodType type = entry.getValue();
            if (type == MethodType.ON_RESPONSE) {
                insertBefore(method, CLASS_NAME + ".beforeComplete($1, $2 != null ? Integer.valueOf($2.code()) : null, $2 != null ? $2.header(\"" + ServletInstrumentation.XTRACE_HEADER + "\") : null, null);", false);
            } else if (type == MethodType.ON_FAILURE){
                insertBefore(method, CLASS_NAME + ".beforeComplete($1, null, null, $2);", false);
            } else {
                logger.warn("Unhandled method " + type + " for OkHttp callback");
            }
        }
        
        return true;
    }
    
    public static void beforeComplete(Object callObject, Integer statusCode, String responseXTrace, Exception exception) {
        if (callObject instanceof SpanAware) {
            Span span = (Span) ((SpanAware) callObject).tvGetSpan();
            if (span != null) {
                if (statusCode != null) {
                    span.setTag("HTTPStatus", statusCode);
                }
                if (responseXTrace != null) {
                    span.setSpanPropertyValue(SpanProperty.CHILD_EDGES, Collections.singleton(responseXTrace));
                }
                
                if (exception != null) {
                    reportError(span, exception);
                }
                
                span.finish();
                
                ((SpanAware) callObject).tvSetSpan(null);
            }
        }
    }
    
    private static String CLASS_NAME = OkHttpCallbackInstrumentation.class.getName();
}