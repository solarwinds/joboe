package com.tracelytics.instrumentation.http.netty;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.http.netty.NettyChannel.ChannelType;

/**
 * Instruments inbound Http2 requests by capturing onHeadersRead operations from `io.netty.handler.codec.http2.Http2FrameListener`
 * @author pluk
 *
 */
public class NettyHttp2FrameListenerInstrumentation extends NettyBaseInstrumentation {

    private static String CLASS_NAME = NettyHttp2FrameListenerInstrumentation.class.getName();

 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("onHeadersRead", new String[] { "io.netty.channel.ChannelHandlerContext", "int", "io.netty.handler.codec.http2.Http2Headers" }, "void", OpType.ON_HEADERS_READ)
    );
    
    private enum OpType {
        ON_HEADERS_READ;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
            
            if (type == OpType.ON_HEADERS_READ) {
                insertBefore(method, CLASS_NAME + ".preOnHeadersRead($1.channel(), $3, Integer.valueOf($2));", false);
            }
        }

        return true;
    }
    
    public static void preOnHeadersRead(Object channelObject, Object headersObject, Integer streamId) {
      //only instruments server channel (not client channel), check is required for http/2 handling as the class of the headersObject does not provide info of whether this is Netty server or client
        if (channelObject instanceof NettyChannel && ((NettyChannel) channelObject).tvGetChannelType() == ChannelType.SERVER) { 
            if (headersObject instanceof NettyHttp2Headers) {
                startTrace(channelObject, headersObject, streamId);
            }
        }
    }
}