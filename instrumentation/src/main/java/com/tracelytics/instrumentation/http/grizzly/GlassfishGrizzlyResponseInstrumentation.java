package com.tracelytics.instrumentation.http.grizzly;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.SpanDictionary;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instruments `org.glassfish.grizzly.http.server.Response#finish` for grizzly span exit.
 *
 * This is a better exit point then the end of the Http Handler exit method as this handles both blocking and
 * non-blocking request handling
 */
public class GlassfishGrizzlyResponseInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = GlassfishGrizzlyResponseInstrumentation.class.getName();
    private static String SPAN_NAME = "glassfish-grizzly";

    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(new MethodMatcher<OpType>("finish", new String[] {}, "void", OpType.FINISH));

    private enum OpType { FINISH }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (Map.Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            insertBefore(methodEntry.getKey(), CLASS_NAME + ".handleExit(this);", false);
        }
        addSpanAware(cc);

        return tagInterface(cc, GlassfishGrizzlyResponse.class.getName());
    }

    public static void handleExit(Object responseObject) {
        GlassfishGrizzlyResponse response = (GlassfishGrizzlyResponse) responseObject;
        Span span = response.tvGetSpan();
        if (span != null) {
            span.setTag("Status", response.getStatus());
            span.finish();

            SpanDictionary.removeSpan(span);
            response.tvSetSpan(null); //clean up to avoid double exit
            Context.clearMetadata();
        }
    }

}