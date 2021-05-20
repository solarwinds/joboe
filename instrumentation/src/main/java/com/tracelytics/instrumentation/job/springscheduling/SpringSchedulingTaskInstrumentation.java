package com.tracelytics.instrumentation.job.springscheduling;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instruments `org.springframework.scheduling.config.Task` for Spring Scheduling Tasks
 *
 * Starts a trace subject to trace decision as entry point.
 *
 * Take note that we have to wrap underlying the runnable object in the task instead of code injection,
 * as declared type of the runnable is `java.lang.Runnable`, we simply cannot instrument all `Runnable`.
 */
public class SpringSchedulingTaskInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = SpringSchedulingTaskInstrumentation.class.getName();

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<OpType>> matchers = Arrays
            .asList(new ConstructorMatcher<OpType>(new String[] { "java.lang.Runnable" }, OpType.CTOR));


    private enum OpType {
        CTOR
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        for (CtConstructor ctor : findMatchingConstructors(cc, matchers).keySet()) {
            insertBefore(ctor, "$1 = " + CLASS_NAME + ".wrapRunnable($1);", false);
        }
        return true;
    }

    public static Runnable wrapRunnable(Runnable runnable) {
        if (!(runnable instanceof RunnableWrapper)) {
            return new RunnableWrapper(runnable);
        } else {
            return runnable;
        }
    }

}
