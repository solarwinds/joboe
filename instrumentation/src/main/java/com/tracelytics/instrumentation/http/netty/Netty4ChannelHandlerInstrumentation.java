package com.tracelytics.instrumentation.http.netty;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Instruments http request and response handled by Netty (4 and later versions).
 * @author pluk
 *
 */
public class Netty4ChannelHandlerInstrumentation extends NettyBaseInstrumentation {

    private static String CLASS_NAME = Netty4ChannelHandlerInstrumentation.class.getName();

 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("channelRead", new String[] { "io.netty.channel.ChannelHandlerContext", "java.lang.Object" }, "void", OpType.READ),
         new MethodMatcher<OpType>("write", new String[] { "io.netty.channel.ChannelHandlerContext", "java.lang.Object" }, "void", OpType.WRITE),
         new MethodMatcher<OpType>("exceptionCaught", new String[] { "io.netty.channel.ChannelHandlerContext", "java.lang.Throwable"}, "void", OpType.EXCEPTION_CAUGHT)
    );

    private enum OpType {
        READ, WRITE, CLOSE, TIMEOUT, EXCEPTION_CAUGHT;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
            
            if (type == OpType.READ) {
                insertBefore(method,
                             "if ($2 instanceof " + NettyHttpRequest.class.getName() + ") {" + //only instruments Http request/header frame to avoid noise
                                  CLASS_NAME + ".startTrace($1.channel(), $2);" +
                             "}", false);

            } else if (type == OpType.WRITE) {
                insertBefore(method,
                            "if ($2 instanceof " + NettyHttpResponse.class.getName() + ") {" + //only instruments Http response/header frame to avoid noise
                                    CLASS_NAME + ".endTrace($1.channel(), $2, false);" +
                            "}", false);
            } else if (type == OpType.EXCEPTION_CAUGHT) {
                insertBefore(method, CLASS_NAME + ".reportException($1.channel(), $2);", false);
            }
        }
        
        

        return true;
    }
}