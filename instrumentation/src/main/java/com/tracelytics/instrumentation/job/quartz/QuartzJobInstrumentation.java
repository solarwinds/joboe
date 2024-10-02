package com.tracelytics.instrumentation.job.quartz;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.EventValueConverter;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;
import com.tracelytics.joboe.span.impl.Tracer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

/**
 * Instruments `org.quartz.Job`.
 *
 * Starts a new trace on job commencement subject to existing trace decision logic.
 *
 */
public class QuartzJobInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = QuartzJobInstrumentation.class.getName();
    private static final String SPAN_NAME = "quartz-job";
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("execute", new String[] { "org.quartz.JobExecutionContext" }, "void", OpType.EXECUTE));

    private static final int RESULT_VALUE_MAX_LENGTH = 256;
    private static final EventValueConverter resultValueConverter = new EventValueConverter(RESULT_VALUE_MAX_LENGTH);
    
    private enum OpType {
        EXECUTE
    }



    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        CtClass jobDetailClass = classPool.get("org.quartz.JobDetail");
        boolean hasGetKeyMethod;
        try {
            jobDetailClass.getMethod("getKey", "()Lorg/quartz/JobKey;");
            hasGetKeyMethod = true;
        } catch (NotFoundException e) {
            hasGetKeyMethod = false;

        }


        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
            
            if (type == OpType.EXECUTE) {
                if (hasGetKeyMethod) {
                    insertBefore(method, CLASS_NAME + ".beforeExecute($1.getJobDetail().getKey(), $1.getJobDetail().getKey().getName(),  $1.getJobDetail().getDescription(), this);", false);
                } else {
                    insertBefore(method, CLASS_NAME + ".beforeExecute($1.getJobDetail().getFullName(), $1.getJobDetail().getName(),  $1.getJobDetail().getDescription(), this);", false);
                }
                addErrorReporting(method, Throwable.class.getName(), SPAN_NAME, classPool, true);

                insertAfter(method, CLASS_NAME + ".afterExecute($1.getResult());", true);
            }
        }
        
        addSpanAware(cc);

        return true;
    }
    
    public static void beforeExecute(Object jobKey, String jobName, String jobDescription, Object jobObject) {
        //Attempts to start/continue trace as this is an entry point
        Tracer.SpanBuilder spanBuilder = getStartTraceSpanBuilder(SPAN_NAME, Collections.<XTraceHeader, String>emptyMap(), jobKey != null ? jobKey.toString() : null, true);

        spanBuilder.withTag("Spec", "job");
        spanBuilder.withTag("Controller", jobObject.getClass().getName());
        spanBuilder.withTag("Action", "execute");

        if (jobKey != null) {
            spanBuilder.withTag("JobKey", jobKey.toString());
            if (jobName != null) {
                spanBuilder.withTag("JobName", jobName);
            }
            if (jobDescription != null) {
                spanBuilder.withTag("JobDescription", jobName);
            }
        }

        Span span = spanBuilder.startActive(true).span();
        span.setTracePropertyValue(TraceProperty.TRANSACTION_NAME, jobKey != null ? "quartz.job." + jobKey.toString() : "quartz.job");
    }
    
    public static void afterExecute(Object result) {
        Scope scope = ScopeManager.INSTANCE.active();
        if (scope != null) {
            if (result != null) {
                scope.span().setTagAsObject("Result", resultValueConverter.convertToEventValue(result));
            }

            scope.close();
        } else {
            logger.warn("Cannot find active scope on job exit!");
        }
    }
}