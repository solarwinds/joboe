package com.tracelytics.instrumentation.http.netty;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.http.netty.NettyChannel.ChannelType;

/**
 * Patches Netty (4 and later versions) channel to provide context and also expose methods of getting remote and local addresses
 * 
 * The channel is also patched to keep track of whether a channel read has been started to avoid duplicated entry event triggered by 
 * multiple cloned Http Request instances spawned from the same Request.
 * 
 * Take note that there's special handling for constructor `io.netty.channel.socket.nio.NioSocketChannel` to identify "server" channel,
 * such that we can instrument ONLY the server handling (vs client handling) of Http/2 headers 
 * 
 * @author pluk
 *
 */
public class Netty4ChannelInstrumentation extends NettyChannelInstrumentation {
    private static final String CLASS_NAME = Netty4ChannelInstrumentation.class.getName();
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<Object>> constructorMatchers = Arrays.asList(
            new ConstructorMatcher<Object>(new String[] { "io.netty.channel.Channel" })
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        super.applyInstrumentation(cc, className, classBytes);
        
        cc.addMethod(CtNewMethod.make("public java.net.SocketAddress getLocalAddress() { return localAddress(); } ", cc));
        cc.addMethod(CtNewMethod.make("public java.net.SocketAddress getRemoteAddress() { return remoteAddress(); } ", cc));
        
        //Flag a channel as "server" channel, this is necessary to correctly instrumentation http/2 headers traffic
        if (cc.getName().equals("io.netty.channel.socket.nio.NioSocketChannel")) {
            for (CtConstructor constructor : findMatchingConstructors(cc, constructorMatchers).keySet()) {
              //if parent is a Server Socket Channel, then this channel is a server channel too
                insertAfter(constructor, "if ($1 instanceof io.netty.channel.socket.nio.NioServerSocketChannel) { " + CLASS_NAME + ".tagChannelAsServer(this); }", true, false); 
            }
        }
        
        return true;
    }
    
    public static void tagChannelAsServer(Object channelObject) {
        if (channelObject instanceof NettyChannel) {
            ((NettyChannel) channelObject).tvSetChannelType(ChannelType.SERVER);
        }
    }
}