package com.appneta.test.proxy;

import io.netty.channel.*;

public class HttpResponseHandler extends ChannelInboundHandlerAdapter {

    private final Channel toSourceChannel;

    public HttpResponseHandler(Channel toSourceChannel) {
        this.toSourceChannel = toSourceChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        toSourceChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    ctx.channel().read();
                } else {
                    future.channel().close();
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        HttpRequestHandler.closeOnFlush(toSourceChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        HttpRequestHandler.closeOnFlush(ctx.channel());
    }
}