package com.tracelytics.instrumentation.http.undertow;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Instruments the <code>io.undertow.io.IoCallback</code> in order to capture IO exit if the Exchange's endExchange is not invoked
 * 
 * @author pluk
 *
 */
public class UndertowIoCallbackInstrumentation extends ClassInstrumentation {
    private static String EXCHANGE_INSTRUMENTATION_CLASS_NAME = UndertowHttpServerExchangeInstrumentation.class.getName();
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(new MethodMatcher<OpType>("onComplete", new String[] { "io.undertow.server.HttpServerExchange" }, "void", OpType.IO_COMPLETE),
                                                                              new MethodMatcher<OpType>("onException", new String[] { "io.undertow.server.HttpServerExchange", "java.lang.Object", "java.io.IOException"}, "void", OpType.IO_EXCEPTION));
    
    private enum OpType {
        IO_COMPLETE, IO_EXCEPTION
    }
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        addTvContextObjectAware(cc);
        
        for (Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            if (entry.getValue() == OpType.IO_COMPLETE) {
                insertBefore(method, EXCHANGE_INSTRUMENTATION_CLASS_NAME + ".traceExit($1, null);", false);
            } else if (entry.getValue() == OpType.IO_EXCEPTION)  {
                insertBefore(method, EXCHANGE_INSTRUMENTATION_CLASS_NAME + ".traceExit($1, $3);", false);
            }
        }
        return true;
    }
}