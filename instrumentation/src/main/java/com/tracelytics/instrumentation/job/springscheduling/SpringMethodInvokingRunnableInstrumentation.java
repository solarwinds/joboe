package com.tracelytics.instrumentation.job.springscheduling;

/**
 * Instruments `org.springframework.scheduling.support.MethodInvokingRunnable`
 */
public class SpringMethodInvokingRunnableInstrumentation extends SpringScheduledRunnableInstrumentation {

    @Override
    protected String getMethodInvocationExpression() {
        return "getTargetMethod()";
    }
}
