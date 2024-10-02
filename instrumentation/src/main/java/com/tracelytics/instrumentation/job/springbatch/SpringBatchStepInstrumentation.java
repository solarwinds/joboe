package com.tracelytics.instrumentation.job.springbatch;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.TraceEventSpanReporter;
import com.tracelytics.joboe.span.impl.Tracer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instruments `org.springframework.batch.core.Step` to create a Span for each step
 */
public class SpringBatchStepInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = SpringBatchStepInstrumentation.class.getName();
    private static final String SPAN_NAME = "spring-batch-step";

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("execute", new String[] { "org.springframework.batch.core.StepExecution" }, "void", OpType.EXECUTE));

    private enum OpType {
        EXECUTE
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {


        for (Map.Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();

            if (type == OpType.EXECUTE) {
                insertBefore(method, CLASS_NAME + ".beforeExecute(getName(), $1.getId());");
                insertAfter(method, CLASS_NAME + ".afterExecute($1.getExitStatus() != null ? $1.getExitStatus().getExitCode() : null);", true);
            }
        }
        return true;
    }



    public static void beforeExecute(String stepName, Long stepExecutionId) {
        Tracer.SpanBuilder spanBuilder = Tracer.INSTANCE.buildSpan(SPAN_NAME).withReporters(TraceEventSpanReporter.REPORTER);
        if (stepName != null) {
            spanBuilder.withTag("StepName", stepName);
        }
        if (stepExecutionId != null) {
            spanBuilder.withTag("StepExecutionId", stepExecutionId);
        }

        spanBuilder.startActive(true);
    }

    public static void afterExecute(String exitStatusCode) {
        Scope scope = ScopeManager.INSTANCE.active();
        if (scope != null) {
            if (exitStatusCode != null) {
                scope.span().setTag("StepExitStatusCode", exitStatusCode);
            }
            scope.close();
        }
    }
}
