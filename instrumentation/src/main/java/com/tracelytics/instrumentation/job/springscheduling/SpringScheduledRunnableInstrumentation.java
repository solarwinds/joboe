package com.tracelytics.instrumentation.job.springscheduling;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * Instruments scheduled runnable of Spring scheduling
 *
 * Starts and ends the trace around the `run` method. Report exception and flag trace as `Error` is unhandled exception is detected
 */
public abstract class SpringScheduledRunnableInstrumentation extends ClassInstrumentation {
    private static final String INSTRUMENTER = SpringSchedulerInstrumenter.class.getName();

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("run", new String[0], "void", OpType.RUN));

    enum OpType { RUN }

    @Override
    protected boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, INSTRUMENTER + ".runEntry(" + getMethodInvocationExpression() + ");", false);
            addErrorReporting(method, RuntimeException.class.getName(), null, classPool, true);
            insertAfter(method, INSTRUMENTER + ".runExit();", true, false);
        }
        return true;
    }

    protected abstract String getMethodInvocationExpression();
}
