package com.tracelytics.instrumentation.http.jetty;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.SpanAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.Span;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

/**
 * Instruments <code>org.eclipse.jetty.client.api.Response$CompleteListener</code> to capture the exit event of
 * asynchronous Jetty http client operation
 */
public class JettyHttpResponseListenerInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = JettyHttpResponseListenerInstrumentation.class.getName();

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
        new MethodMatcher<MethodType>("onComplete", new String[]{ "org.eclipse.jetty.client.api.Result" } , "void", MethodType.ON_COMPLETE, true)
    );
    
    private enum MethodType {
        ON_COMPLETE
    }
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        addSpanAware(cc);

        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            insertBefore(method, CLASS_NAME + ".beforeOnComplete(this, $1.getResponse() != null ? Integer.valueOf($1.getResponse().getStatus()) : null, $1.getResponse() != null ? $1.getResponse().getHeaders().get(\"" + XTRACE_HEADER + "\") : null, $1.getFailure());", false);
        }

        tagInterface(cc, JettyHttpRequest.class.getName());
        
        return true;
    }
  

    public static void beforeOnComplete(Object listenerObject, Integer status, String responseXTrace, Throwable failure) {
        SpanAware listener = (SpanAware) listenerObject;

        if (listener.tvGetSpan() != null) {
            Scope scope = tracer.activateSpan(listener.tvGetSpan());
            Span span = scope.span();
            if (failure != null) {
                reportError(span, failure);
            }
            if (status != null && status != 0) {
                span.setTag("HTTPStatus", status);
            }
            if (responseXTrace != null) {
                try {
                    Metadata responseMetadata = new Metadata(responseXTrace);
                    if (responseMetadata.isTaskEqual(Context.getMetadata())) {
                        span.setSpanPropertyValue(Span.SpanProperty.CHILD_EDGES, Collections.singleton(responseXTrace));
                    }
                } catch (OboeException e) {
                    logger.debug("Found invalid response x-trace ID from jetty http instrumentation : [" + responseXTrace + "]");
                }

            }

            span.finish();
            scope.close();
        }
    }
}