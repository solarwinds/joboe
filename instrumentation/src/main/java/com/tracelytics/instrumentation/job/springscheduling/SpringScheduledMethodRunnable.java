package com.tracelytics.instrumentation.job.springscheduling;

import java.lang.reflect.Method;

public interface SpringScheduledMethodRunnable {
    Method getMethod();
}
