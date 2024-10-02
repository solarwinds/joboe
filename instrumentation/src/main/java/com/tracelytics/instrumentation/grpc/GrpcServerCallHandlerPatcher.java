package com.tracelytics.instrumentation.grpc;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.*;
import com.tracelytics.joboe.Context;

/**
 * Patches `io.grpc.ServerCallHandler` to propagates span and context started by grpc-server entry to `io.grpc.ServerCall$Listener`
 *   
 * @author Patson
 *
 */
public class GrpcServerCallHandlerPatcher extends ClassInstrumentation {
    private static final String CLASS_NAME = GrpcServerCallHandlerPatcher.class.getName();

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("startCall", new String[] { "io.grpc.ServerCall", "io.grpc.Metadata" }, "io.grpc.ServerCall$Listener", OpType.START));

    
    private enum OpType {
        START;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {

        for (CtMethod startMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertAfter(startMethod, CLASS_NAME + ".postStartCall($1, $_);", true, false);
        }

        return true;
    }

 
    public static void postStartCall(Object serverCallObject, Object listenerObject) {
        if (serverCallObject instanceof SpanAware && listenerObject instanceof TvContextObjectAware) {
            SpanAware serverCall = (SpanAware) serverCallObject;
            if (serverCall.tvGetSpan() != null) {
                ((TvContextObjectAware) listenerObject).setTvContext(serverCall.tvGetSpan().context().getMetadata());
            }
        }
        
        Context.clearMetadata();
    }
}