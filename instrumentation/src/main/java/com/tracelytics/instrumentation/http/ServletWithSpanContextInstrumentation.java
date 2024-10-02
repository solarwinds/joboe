package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.span.impl.LogEntry;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.SpanDictionary;
import com.tracelytics.joboe.span.impl.SpanReporter;

import java.util.Arrays;
import java.util.List;

/**
 * Modifies the service method of servlet, such that the span id is included in the request headers and the span is added to dictionary for lookup later. 
 * 
 * This is useful for passing span context for MVC frameworks that runs on different threads (For example Play, Spray). 
 * 
 * The most reliable way to propagate span to those threads is via span key as request header, which are guaranteed to be consistent even across threads on the same request
 * 
 * @author pluk
 *
 */
public class ServletWithSpanContextInstrumentation extends ServletInstrumentation {
    public static final String CLASS_NAME = ServletWithSpanContextInstrumentation.class.getName();
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(
        new MethodMatcher<Object>("service", new String[] { "javax.servlet.ServletRequest", "javax.servlet.ServletResponse"}, "void")
            );
    
    @Override
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, CLASS_NAME + ".storeSpanContext($1);", false);
        }
        // Note that the order matters: the instrumentation of the super class (ServletInstrumentation) must be applied **after**
        // this class because `insertBefore` always inject the patch code on the top of the existing code. This ensures the patch
        // injected in the super class (which starts the span) happens before the patch in this class (fetch the span key and store
        // it into the extra header.
        // We want the span creation in super instrumentation to happen first so the storeSpanContext can grab the span created
        super.applyInstrumentation(cc, className, classBytes);

        return true;
    }
    
    public static void storeSpanContext(final Object req) {
        if (req instanceof HttpServletRequest) {
            Span currentSpan = ScopeManager.INSTANCE.activeSpan();
            if (currentSpan != null) {
                final long spanKey = SpanDictionary.setSpan(currentSpan);
                ((HttpServletRequest) req).tvSetExtraHeader(ClassInstrumentation.X_SPAN_KEY, String.valueOf(spanKey));
                
                //add a cleanup actor to remove the header and span entry from dictionary
                currentSpan.addSpanReporter(new SpanReporter() {
                    @Override
                    public void reportOnStart(Span span) {
                    }
                    
                    @Override
                    public void reportOnLog(Span span, LogEntry logEntry) {
                    }
                    
                    @Override
                    public void reportOnFinish(Span span, long finishMicroSec) {
                        ((HttpServletRequest) req).tvRemoveExtraHeader(ClassInstrumentation.X_SPAN_KEY); //remove the header as some servlet impl might reuse the request object instance
                        SpanDictionary.removeSpan(spanKey);
                    }
                });
            }
        }
    }
}
