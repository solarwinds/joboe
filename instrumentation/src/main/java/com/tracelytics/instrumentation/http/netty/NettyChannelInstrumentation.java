package com.tracelytics.instrumentation.http.netty;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.http.netty.NettyChannel.ChannelType;
import com.tracelytics.joboe.span.impl.Span;

/**
 * Patches Netty (3 and earlier versions) channel to provide context and also expose methods of getting remote and local addresses
 * 
 * The channel is also patched to keep track of whether a channel read has been started to avoid duplicated entry event triggered by 
 * multiple cloned Http Request instances spawned from the same Request.
 * 
 * This channel might also contain `ChannelType` information (which is only meaningful for Http/2 handling) such that we can selectively
 * instrument Http/2 headers operations ONLY on server side 
 * 
 * @author pluk
 *
 */
public class NettyChannelInstrumentation extends NettyBaseInstrumentation {
    private static String CLASS_NAME = NettyChannelInstrumentation.class.getName();
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("close", new String[] { }, "org.jboss.netty.channel.ChannelFuture", OpType.CLOSE), //netty 3
         new MethodMatcher<OpType>("doClose", new String[] { }, "void", OpType.CLOSE) //netty 4
    );
    
    private enum OpType {
        CLOSE;
    }
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        cc.addField(CtField.make("private " + Span.class.getName() + " tvSpan;", cc));
        cc.addField(CtField.make("private java.util.concurrent.ConcurrentHashMap tvSpansByStreamId = new java.util.concurrent.ConcurrentHashMap();", cc));

        cc.addMethod(CtNewMethod.make("public void tvSetSpan(Integer streamId, " + Span.class.getName() + " span) { "
                + "    if (streamId == null) {"
                + "        tvSpan = span; "
                + "    } else {"
                + "        tvSpansByStreamId.put(streamId, span);"
                + "    }"
                + "}", cc));
        cc.addMethod(CtNewMethod.make("public " + Span.class.getName() + " tvGetSpan(Integer streamId) {"
                + "    if (streamId == null) {"
                + "        return tvSpan; "
                + "    } else {"
                + "        return (" + Span.class.getName() + ") tvSpansByStreamId.get(streamId);"
                + "    }"
                + "}", cc));
        
        cc.addMethod(CtNewMethod.make("public " + Span.class.getName() + " tvGetAndRemoveSpan(Integer streamId) {"
                + "    if (streamId == null) {"
                +          Span.class.getName() + " span = tvSpan;"
                + "        tvSpan = null;"
                + "        return span; "
                + "    } else {"
                + "        return (" + Span.class.getName() + ") tvSpansByStreamId.remove(streamId);"
                + "    }"
                + "}", cc));
        
        cc.addMethod(CtNewMethod.make("public java.util.Map tvGetAndRemoveAllSpans() { "
                                    + "    java.util.Map allSpans = new java.util.HashMap(tvSpansByStreamId);"
                                    + "    tvSpansByStreamId.clear();"
                                    + "    if (tvSpan != null) {"
                                    + "        allSpans.put(null, tvSpan);"
                                    + "        tvSpan = null;"
                                    + "    }"
                                    + "    return allSpans;"
                                    + "}", cc));
        
        cc.addField(CtField.make("private java.util.Map tvReportedExceptions = new java.util.WeakHashMap();", cc)); //for jdk 1.5 cannot use Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());
        cc.addMethod(CtNewMethod.make("public boolean tvHasExceptionReported(Throwable exception) { return tvReportedExceptions.containsKey(exception); }", cc));
        cc.addMethod(CtNewMethod.make("public void tvAddExceptionReported(Throwable exception) { tvReportedExceptions.put(exception, Boolean.TRUE); }", cc));

        cc.addField(CtField.make("private " + ChannelType.class.getName() + " tvChannelType;", cc));
        cc.addMethod(CtNewMethod.make(
                "public " + ChannelType.class.getName() + " tvGetChannelType() { "
              + "    return tvChannelType;"
              + "}", cc));
        
        cc.addMethod(CtNewMethod.make(
                "public void tvSetChannelType(" + ChannelType.class.getName() + " channelType) { "
              + "    tvChannelType = channelType;"
              + "}", cc));
        
        tagInterface(cc, NettyChannel.class.getName());
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, CLASS_NAME + ".endTrace(this, null, true);", false); //ends all traces within this channel if `close` is invoked
        }

        return true;
    }
}