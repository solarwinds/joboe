package com.tracelytics.instrumentation.grpc;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Patches `io.grpc.Channel` to provide information on the remote host this channel connected to 
 * @author Patson
 *
 */
public class GrpcChannelPatcher extends ClassInstrumentation {
    private static final String CLASS_NAME = GrpcChannelPatcher.class.getName();
    
 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("newCall", new String[] {}, "io.grpc.ClientCall", OpType.NEW_CALL));

    private enum OpType {
        NEW_CALL;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {

        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertAfter(method, CLASS_NAME + ".setTarget($_, authority());");
        }

        addSpanAware(cc);
        return true;
    }

    public static void setTarget(Object clientCallObject, String authority) {
        if (clientCallObject instanceof GrpcClientCall) { //it might not be a GrpcClientCall as only ClientCallImpl is patched
            ((GrpcClientCall) clientCallObject).tvSetTarget(authority);
        } 
   }
}