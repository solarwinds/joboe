package com.tracelytics.instrumentation.threadpool;

import com.tracelytics.ext.google.common.collect.MapMaker;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ContextPropagationPatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.SpanAware;
import com.tracelytics.instrumentation.SpanAwareImpl;
import com.tracelytics.instrumentation.TraceContextData;
import com.tracelytics.instrumentation.TvContextObjectAwareImpl;
import com.tracelytics.joboe.span.impl.Span;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Patches `ForkJoinTask` such that it can carry context, then restore/reset before and after the task exectuion
 * @author pluk
 *
 */
public class ForkJoinTaskPatcher extends ClassInstrumentation {
    private static String CLASS_NAME = ForkJoinTaskPatcher.class.getName();
    private static Map<Object, TraceContextData> contextDataMap = new MapMaker().weakKeys().makeMap();
    
    private enum OpType { EXECUTE, FORK }
    
 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("doExec", new String[] { }, "void", OpType.EXECUTE), //JDK 7
            new MethodMatcher<OpType>("doExec", new String[] { }, "int", OpType.EXECUTE), //JDK 8
            new MethodMatcher<OpType>("fork", new String[] { }, "java.util.concurrent.ForkJoinTask", OpType.FORK)); 
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
    	for (Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
    	    CtMethod method = entry.getKey();
    	    OpType type = entry.getValue();
    	    if (type == OpType.EXECUTE) {
        	    insertBefore(method, CLASS_NAME + ".restoreContext(this);", false);
        	    insertAfter(method, CLASS_NAME + ".resetContext(this);", true, false);
    	    } else if (type == OpType.FORK) {
    	      //when fork is invoked on the task, it should capture the current context, such that it can be restored when the forked task is excuted
    	        insertBefore(method, CLASS_NAME + ".captureContext(this);", false); 
    	    }
        }
    	
    	return true;
    }
    
    public static void restoreContext(Object task) {
        ContextPropagationPatcher.restoreContext(contextDataMap.get(task));
    }
    
    public static void resetContext(Object task) {
        ContextPropagationPatcher.resetContext(contextDataMap.get(task));
    }
    
    public static void captureContext(Object task) {
        TraceContextData contextData = new TraceContextData();
        ContextPropagationPatcher.captureContext(contextData);
        contextDataMap.put(task, contextData);
    }
}

    