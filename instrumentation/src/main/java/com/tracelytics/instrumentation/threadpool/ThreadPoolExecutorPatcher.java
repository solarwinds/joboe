package com.tracelytics.instrumentation.threadpool;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.google.common.collect.MapMaker;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ContextObjectAndSpanAwareImpl;
import com.tracelytics.instrumentation.ContextPropagationPatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;

public class ThreadPoolExecutorPatcher extends ClassInstrumentation {

    private static String CLASS_NAME = ThreadPoolExecutorPatcher.class.getName();
    private static Map<Object, TvContextObjectAware> taskContextMap; //lazily initialized, see https://github.com/librato/joboe/issues/693
    
    private enum OpType { EXECUTE, ADD_WORKER, ADD_THREAD, BEFORE_EXECUTE, AFTER_EXECUTE }
    
 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(new MethodMatcher<OpType>("execute", new String[] { "java.lang.Runnable" }, "void", OpType.EXECUTE),
                                                                              new MethodMatcher<OpType>("addWorker", new String[] { "java.lang.Runnable", "boolean" }, "boolean", OpType.ADD_WORKER), //jdk 1.7 - 1.8
                                                                              new MethodMatcher<OpType>("addThread", new String[] { "java.lang.Runnable" }, "java.lang.Thread", OpType.ADD_THREAD), //jdk 1.5 - 1.6
                                                                //              new MethodMatcher<OpType>("decorateTask", new String[] { }, "java.util.concurrent.RunnableScheduledFuture", OpType.SCHEDULE), //java.util.concurrent.ScheduledExecutorService
                                                                              new MethodMatcher<OpType>("beforeExecute", new String[] { "java.lang.Thread", "java.lang.Runnable" }, "void", OpType.BEFORE_EXECUTE),
                                                                              new MethodMatcher<OpType>("afterExecute", new String[] { "java.lang.Runnable", "java.lang.Throwable" }, "void", OpType.AFTER_EXECUTE));
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            OpType type = entry.getValue();
            CtMethod method = entry.getKey();
            if (type == OpType.EXECUTE) {
                insertBefore(method, CLASS_NAME + ".captureContext($1);", false);
//          } else if (type == OpType.SCHEDULE) {
//              insertAfter(method, CLASS_NAME + ".captureContext($_);", true);
            } else if (type == OpType.ADD_WORKER || type == OpType.ADD_THREAD) {
                insertBefore(method, Context.class.getName() + ".setSkipInheritingContext(true);", false); //tell Context to skip cloning context on the spawned thread as it is better handled in this instrumentation
                insertAfter(method, Context.class.getName() + ".setSkipInheritingContext(false);", true, false); //reset the flag to resume the default
            } else if (type == OpType.BEFORE_EXECUTE) {
                insertBefore(method, CLASS_NAME + ".restoreContext($2);", false);
            } else if (type == OpType.AFTER_EXECUTE) {
                insertAfter(method, CLASS_NAME + ".resetContext($1);", true, false);
            }
            
            
        }
        return true;
    }
    
    public static void captureContext(Object task) {
        if (task != null && Context.isValid()) {
            ContextObjectAndSpanAwareImpl context = new ContextObjectAndSpanAwareImpl();
            context.setTvContext(Context.getMetadata());
            context.setTvFromThreadId(Thread.currentThread().getId());
            
            Span activeSpan = ScopeManager.INSTANCE.activeSpan();
            if (activeSpan != null && activeSpan.getSpanPropertyValue(SpanProperty.IS_SDK)) { //only propagate SDK span for now
                context.tvSetSpan(activeSpan);
            }
            getMap().put(task, context);
        }
    }
    
    public static boolean restoreContext(Object task) {
        if (getMap().containsKey(task)) {
            return ContextPropagationPatcher.restoreContext(getMap().get(task));
        }
        return false;
    }
    
    public static void resetContext(Object task) {
        if (getMap().containsKey(task)) {
            TvContextObjectAware context = getMap().get(task);
          //do not attempt to reset to previous context, instead always clear the context
          //as in java a thread is supposed to completely finish each task (whether success or failure) before being recycled and used for a different task. 
          //Therefore it does not make sense to "reset" to previous context after a task is finished
            ContextPropagationPatcher.resetContext(context, false); 
            getMap().remove(task);
        }
    }
    
    /**
     * Lazily initialize it. Otherwise might run into ClassCircularityError, see https://github.com/librato/joboe/issues/693
     * @return
     */
    private static synchronized Map<Object, TvContextObjectAware> getMap() {
        if (taskContextMap == null) {
            taskContextMap = new MapMaker().weakKeys().makeMap();
        }
        
        return taskContextMap;
    }
    
}

    