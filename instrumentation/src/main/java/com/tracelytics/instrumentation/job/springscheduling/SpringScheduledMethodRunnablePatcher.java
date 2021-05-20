package com.tracelytics.instrumentation.job.springscheduling;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Patches the `org.springframework.scheduling.support.ScheduledMethodRunnable` so we can invoke `getMethod`
 */

public class SpringScheduledMethodRunnablePatcher extends ClassInstrumentation {
    @Override
    protected boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        return tagInterface(cc, SpringScheduledMethodRunnable.class.getName());
    }
}
