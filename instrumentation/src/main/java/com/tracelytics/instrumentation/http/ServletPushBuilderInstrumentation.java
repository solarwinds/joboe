package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.HeaderConstants;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Tracer;

import java.util.Arrays;
import java.util.List;

/**
 * Instruments `javax.servlet.http.PushBuilder` for the Server Push operation from Servlet 4.0.
 *
 * A span is added when `push` is invoked https://javaee.github.io/javaee-spec/javadocs/javax/servlet/http/PushBuilder.html#push--
 *
 * Take note that the push call itself is non-blocking and should be quite short, we are reporting it to keep track of the "pushed" path,
 *
 * such that it can be matched with the nested async span - the processing done for the "pushed" path, which is just another regular servlet triggered by the app container internally.
 *
 * Take note that in order to link the nested async span, we will need to invoke `PushBuilder#setHeader` to set the x-trace header when the app container forwards the request.
 *
 * @author pluk
 */
public class ServletPushBuilderInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = ServletPushBuilderInstrumentation.class.getName();
    private static final String SPAN_NAME = "server-push";
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("push", new String[] { }, "void", OpType.PUSH)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (CtMethod pushMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            //has to insert BEFORE the complete() call, otherwise some newer servlet implementation might throw except for calling getRequest() after complete()
            insertBefore(pushMethod, "setHeader(\"" + HeaderConstants.XTRACE_HEADER + "\", " + CLASS_NAME +  ".pushEntry(this.getPath()));", false);
            insertAfter(pushMethod, CLASS_NAME + ".pushExit();", true);
        }
        
        return true;
    }
    
    public static String pushEntry(String path) {
        if (Context.getMetadata().isSampled()) {
            Tracer.SpanBuilder spanBuilder = buildTraceEventSpan(SPAN_NAME);
            spanBuilder.withTag("PushPath", path);
            spanBuilder.startActive();
        }
        return Context.getMetadata().toHexString();
    }

    public static void pushExit() {
        Scope scope = ScopeManager.INSTANCE.active();
        if (scope == null || scope.span() == null) {
            logger.warn("Attempt to close span with operation name [" + SPAN_NAME + "] but found no scope/span");
        } else if (!SPAN_NAME.equals(scope.span().getOperationName())) {
            logger.warn("Attempt to close span with operation name [" + SPAN_NAME + "] but found " + scope.span().getOperationName());
        } else {
            scope.close();
        }
    }
     
    
    private enum OpType {
        PUSH
    }
}
