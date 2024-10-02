package com.tracelytics.instrumentation.threadpool;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;

import java.util.Arrays;
import java.util.List;

/**
 * Patches `io.netty.util.concurrent.DefaultThreadFactory` to avoid context inheritance, as such a thread factory appears
 * to create threads for pool. Pool threads are usually reused hence should not inherit the context from the initial
 * creator.
 *
 * @author pluk
 *
 */
public class NettyDefaultThreadFactoryPatcher extends ClassInstrumentation {
    private enum OpType { NEW_THREAD }

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("newThread", new String[] { "java.lang.Runnable" }, "java.lang.Thread", OpType.NEW_THREAD)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, Context.class.getName() + ".setSkipInheritingContext(true);", false);
            insertAfter(method, Context.class.getName() + ".setSkipInheritingContext(false);", true, false);
        }
    	return true;
    }
}

    