package com.tracelytics.instrumentation.grpc;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Patches `io.grpc.stub.ClientCalls` to flag whether the current `io.grpc.ClientCall` is invoked from a synchronous or asynchronous manner.
 * Such that when the `io.grpc.ClientCall` is instrumented, it can set the async flag properly
 * 
 * @author pluk
 *
 */
public class GrpcClientCallsPatcher extends ClassInstrumentation {
    private static final String CLASS_NAME = GrpcClientCallsPatcher.class.getName();
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("asyncUnaryCall", new String[] { }, "void", OpType.ASYNC),
         new MethodMatcher<OpType>("asyncServerStreamingCall", new String[] { }, "void", OpType.ASYNC),
         new MethodMatcher<OpType>("asyncClientStreamingCall", new String[] { }, "java.lang.Object", OpType.ASYNC),
         new MethodMatcher<OpType>("asyncBidiStreamingCall", new String[] { }, "java.lang.Object", OpType.ASYNC),
         new MethodMatcher<OpType>("blockingUnaryCall", new String[] { }, "java.lang.Object", OpType.BLOCKING),
         new MethodMatcher<OpType>("blockingServerStreamingCall", new String[] { }, "java.lang.Object", OpType.BLOCKING),
         new MethodMatcher<OpType>("futureUnaryCall", new String[] { }, "java.lang.Object", OpType.FUTURE)
         
    );
    
    private enum OpType {
        ASYNC, BLOCKING, FUTURE;
    }
    
    private static final ThreadLocal<Integer> callDepthThreadLocal = new ThreadLocal<Integer>();
    
    private static final ThreadLocal<Boolean> isAsyncThreadLocal = new ThreadLocal<Boolean>();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        for (Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            OpType opType = entry.getValue();
            
            if (opType == OpType.BLOCKING) {
                insertBefore(method, CLASS_NAME + ".preStart(false);", false);
                insertAfter(method, CLASS_NAME + ".postStart();", true, false);
            } else if (opType == OpType.ASYNC || opType == OpType.FUTURE) {
                insertBefore(method, CLASS_NAME + ".preStart(true);", false);
                insertAfter(method, CLASS_NAME + ".postStart();", true, false);
            }
        }
       
        return true;
    }
    
    public static void preStart(boolean isAsync) {
        if (callDepthThreadLocal.get() == null) { //first call, set async flag
            callDepthThreadLocal.set(1);
            isAsyncThreadLocal.set(isAsync);
        } else {
            callDepthThreadLocal.set(callDepthThreadLocal.get() + 1);
        }
    }
    
    public static void postStart() {
        Integer callDepth = callDepthThreadLocal.get(); 
        if (callDepth != null) {
            if (callDepth == 1) { //first call, cleanup after
                callDepthThreadLocal.remove();
                isAsyncThreadLocal.remove();
            } else {
                callDepthThreadLocal.set(callDepth - 1);
            }
        }
    }
    
    public static boolean isAsync() {
        return isAsyncThreadLocal.get() != null && isAsyncThreadLocal.get();
    }
}