package com.tracelytics.instrumentation.job.springbatch;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instruments `org.springframework.batch.core.StepExecution` to report failure exception
 */
public class SpringBatchStepExecutionInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = SpringBatchStepExecutionInstrumentation.class.getName();
    private static final String SPAN_NAME = "spring-batch-step";

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("addFailureException", new String[] { "java.lang.Throwable" }, "void", OpType.ADD_FAILURE_EXCEPTION));

    private enum OpType {
        ADD_FAILURE_EXCEPTION
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {


        for (Map.Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();

            if (type == OpType.ADD_FAILURE_EXCEPTION) {
                insertBefore(method, CLASS_NAME + ".beforeAddFailureException($1);");
            }
        }
        return true;
    }

    public static void beforeAddFailureException(Throwable throwable) {
        reportError(SPAN_NAME, throwable);
    }
}
