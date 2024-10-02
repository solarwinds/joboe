package com.tracelytics.instrumentation.http.netty;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Instruments http request and response handled by Netty (3 and earlier versions).
 * @author pluk
 *
 */
public class Netty3ChannelHandlerInstrumentation extends NettyBaseInstrumentation {

    private static String CLASS_NAME = Netty3ChannelHandlerInstrumentation.class.getName();

 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("handleUpstream", new String[] { "java.lang.Object" }, "void", OpType.READ),
         new MethodMatcher<OpType>("handleDownstream", new String[] { "java.lang.Object" }, "void", OpType.WRITE),
         new MethodMatcher<OpType>("exceptionCaught", new String[] { "org.jboss.netty.channel.ChannelHandlerContext", "org.jboss.netty.channel.ExceptionEvent"}, "void", OpType.EXCEPTION_CAUGHT)
         
    );

    private enum OpType {
        READ, WRITE, EXCEPTION_CAUGHT;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();

            if (shouldModify(cc, method)) {

                if (type == OpType.READ) {
                    insertBefore(method,
                                 "if ($2 instanceof org.jboss.netty.channel.MessageEvent) {" +
                                 "   Object message = ((org.jboss.netty.channel.MessageEvent)$2).getMessage();" +
                                 "   if (message instanceof " + NettyHttpRequest.class.getName() + ") {" + //only instruments Http request to avoid noise
                                         CLASS_NAME + ".startTrace($2.getChannel(), message);" +
                                 "   }" +
                                 "}", false);

                } else if (type == OpType.WRITE) {
                    insertBefore(method,
                                 "if ($2 instanceof org.jboss.netty.channel.MessageEvent) {" +
                                 "   Object message = ((org.jboss.netty.channel.MessageEvent)$2).getMessage();" +
                                 "   if (message instanceof " + NettyHttpResponse.class.getName() + ") {" + //only instruments Http request to avoid noise
                                         CLASS_NAME + ".endTrace($2.getChannel(), message, false);" +
                                 "   }" +
                                 "}", false);
                } else if (type == OpType.EXCEPTION_CAUGHT) {
                    insertBefore(method, CLASS_NAME + ".reportException($1 != null ? $1.getChannel() : null, $2 != null ? $2.getCause() : null);", false);
                }


            }
        }

        return true;
    }
}