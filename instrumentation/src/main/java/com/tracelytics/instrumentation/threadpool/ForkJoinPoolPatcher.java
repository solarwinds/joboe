package com.tracelytics.instrumentation.threadpool;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

/**
 * Patches `ForkJoinPool` such that when `ForkJoinTask` was called/triggered from non-fork/join client, 
 * the current context will be captured and inserted into the `ForkJoinTask` instance. 
 * Such that when the `ForkJoinTask` is run, context can be restored on the worker thread
 *   
 * @author pluk
 *
 */
public class ForkJoinPoolPatcher extends ClassInstrumentation {
    private static String CLASS_NAME = ForkJoinPoolPatcher.class.getName();
    
    private enum OpType { FORK_OR_SUBMIT, INVOKE, EXTERNAL_PUSH }
    
 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("forkOrSubmit", new String[] { "java.util.concurrent.ForkJoinTask" }, "void", OpType.FORK_OR_SUBMIT), //1.7
            new MethodMatcher<OpType>("invoke", new String[] { "java.util.concurrent.ForkJoinTask" }, "java.lang.Object", OpType.INVOKE), //1.7
            new MethodMatcher<OpType>("externalPush", new String[] { "java.util.concurrent.ForkJoinTask" }, "void", OpType.EXTERNAL_PUSH) //1.8
            );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
    	for (Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
    	    CtMethod method = entry.getKey();
  	        insertBefore(method, CLASS_NAME + ".captureContext($1);", false);
        }
    	return true;
    }
    
    public static void captureContext(Object task) {
        ForkJoinTaskPatcher.captureContext(task);
    }
}

    