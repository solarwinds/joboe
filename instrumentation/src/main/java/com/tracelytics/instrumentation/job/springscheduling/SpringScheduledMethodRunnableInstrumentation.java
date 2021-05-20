package com.tracelytics.instrumentation.job.springscheduling;

/**
 * Instruments `org.springframework.scheduling.support.ScheduledMethodRunnable`
 */
public class SpringScheduledMethodRunnableInstrumentation extends SpringScheduledRunnableInstrumentation {

    @Override
    protected String getMethodInvocationExpression() {
        return "getMethod()";
    }
}
