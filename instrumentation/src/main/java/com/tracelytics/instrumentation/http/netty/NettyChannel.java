package com.tracelytics.instrumentation.http.netty;

import java.net.SocketAddress;
import java.util.Map;

import com.tracelytics.joboe.span.impl.Span;

public interface NettyChannel {
    SocketAddress getLocalAddress();
    SocketAddress getRemoteAddress();

    boolean tvHasExceptionReported(Throwable exception);
    void tvAddExceptionReported(Throwable exception);
    
    void tvSetSpan(Integer streamId, Span span);
    Span tvGetSpan(Integer streamId);
    Span tvGetAndRemoveSpan(Integer streamId);
    
    Map<Integer, Span> tvGetAndRemoveAllSpans();
    
    ChannelType tvGetChannelType();
    void tvSetChannelType(ChannelType channelType);

    public enum ChannelType { CLIENT, SERVER } //CLIENT is not used for now, no sure fire way to identify CLIENT
    
}