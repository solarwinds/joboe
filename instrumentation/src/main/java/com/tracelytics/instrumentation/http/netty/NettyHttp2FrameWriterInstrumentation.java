package com.tracelytics.instrumentation.http.netty;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.http.netty.NettyChannel.ChannelType;

/**
 * Instruments outbound Http2 response by capturing writeHeaders operations from `io.netty.handler.codec.http2.Http2FrameWriter`  
 * @author pluk
 *
 */
public class NettyHttp2FrameWriterInstrumentation extends NettyBaseInstrumentation {

    private static String CLASS_NAME = NettyHttp2FrameWriterInstrumentation.class.getName();

 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("writeHeaders", new String[] { "io.netty.channel.ChannelHandlerContext", "int", "io.netty.handler.codec.http2.Http2Headers" }, "io.netty.channel.ChannelFuture", OpType.WRITE_HEADERS)
    );

    private enum OpType {
        WRITE_HEADERS;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
            
            if (type == OpType.WRITE_HEADERS) {
                insertBefore(method, CLASS_NAME + ".preWriteHeaders($1.channel(), $3, Integer.valueOf($2));", false);
            }
        }

        return true;
    }
    
    public static void preWriteHeaders(Object channelObject, Object headersObject, Integer streamId) {
        //only instruments server channel (not client channel), check is required for http/2 handling as the class of the headersObject does not provide info of whether this is Netty server or client
        if (channelObject instanceof NettyChannel && ((NettyChannel) channelObject).tvGetChannelType() == ChannelType.SERVER) {
            if (headersObject instanceof NettyHttp2Headers) {
                endTrace(channelObject, headersObject, false, streamId);
            }
        }
    }
}