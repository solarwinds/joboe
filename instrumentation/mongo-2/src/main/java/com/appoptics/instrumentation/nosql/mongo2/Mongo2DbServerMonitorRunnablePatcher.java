package com.appoptics.instrumentation.nosql.mongo2;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;

import java.util.Arrays;
import java.util.List;

/**
 * Patches the com.mongodb.ServerMonitor$ServerMonitorRunnable such that the thread used by the runnable would not trigger events.
 * 
 * This is to avoid tracing events triggered by the ServerMonitor which runs continously in the background
 * 
 * @author pluk
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "com.mongodb.ServerMonitor$ServerMonitorRunnable", module = Module.MONGODB)
public class Mongo2DbServerMonitorRunnablePatcher extends Mongo2BaseInstrumentation {
    private static final String CLASS_NAME = Mongo2DbServerMonitorRunnablePatcher.class.getName();

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(
                                                                                     new MethodMatcher<Object>("run", new String[] { }, "void")
                                                                             );

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, CLASS_NAME + ".clearMetadata();"); 
        }

        return true;
    }
    
    public static void clearMetadata() {
        Context.clearMetadata();//clear the current thread context before run, do not capture anything from this thread spawned by ServerMonitor
    }
}