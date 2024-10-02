package com.tracelytics.instrumentation.job.springbatch;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Tracer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Instruments `org.springframework.batch.core.Job` and starts a trace subject to trace decision as entry point
 */
public class SpringBatchJobInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = SpringBatchJobInstrumentation.class.getName();
    private static final String SPAN_NAME = "spring-batch-job";

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("execute", new String[] { "org.springframework.batch.core.JobExecution" }, "void", OpType.EXECUTE));


    private enum OpType {
        EXECUTE
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        for (Map.Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();

            if (type == OpType.EXECUTE) {
                insertBefore(method, CLASS_NAME + ".beforeExecute(getName(), this, $1.getId());", false);
                insertAfter(method, CLASS_NAME + ".afterExecute($1.getExitStatus() != null ? $1.getExitStatus().getExitCode() : null);", true);
            }
        }

        addSpanAware(cc);

        return true;
    }

    public static void beforeExecute(String jobName, Object jobObject, Long jobExecutionId) {
        //Attempts to start/continue trace as this is an entry point
        Tracer.SpanBuilder spanBuilder = getStartTraceSpanBuilder(SPAN_NAME, Collections.<XTraceHeader, String>emptyMap(), jobName, true);

        spanBuilder.withTag("Controller", jobObject.getClass().getName());
        spanBuilder.withTag("Action", "execute");
        spanBuilder.withTag("Spec", "job");
        if (jobName != null) {
            spanBuilder.withTag("JobName", jobName);
        }
        if (jobExecutionId != null) {
            spanBuilder.withTag("JobExecutionId", jobExecutionId);
        }

        Span span = spanBuilder.startActive(true).span();
        span.setTracePropertyValue(Span.TraceProperty.TRANSACTION_NAME, "spring.batch." + (jobName != null ? jobName : jobObject.getClass().getName()));
    }


    public static void afterExecute(String exitStatusCode) {
        Scope scope = ScopeManager.INSTANCE.active();
        if (scope != null) {
            if (exitStatusCode != null) {
                scope.span().setTag("JobExitStatusCode", exitStatusCode);
                if ("FAILED".equals(exitStatusCode)) {
                    scope.span().setTracePropertyValue(Span.TraceProperty.HAS_ERROR, true);
                }
            }
            scope.close();
        } else {
            logger.warn("Cannot find active scope on SpringBatchJobInstrumentation#afterExecute!");
        }
    }
}
