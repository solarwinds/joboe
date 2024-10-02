package com.appneta.test.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

/**
 * Handles Http Request traffic between the source (the one that issues the HTTP request) and this proxy, and relay
 * this request to the target server
 *
 * Upon activation of the channel which this handler bound to - the inbound channel - between the source and this proxy:
 * 1. Create an outbound channel that connects to the target server using HttpOutboundChannelInitializer, also pass the inbound channel
 * to this initializer
 * 2. The HttpOutboundChannelInitializer will assign a HttpResponseHandler to handle the http response coming back from the target server
 * and then it will relay it back to the source using the inbound channel
 *
 *
 */
public class HttpRequestHandler extends ChannelInboundHandlerAdapter {
    private final String remoteHost;
    private final int remotePort;

    //extra channel from this proxy to target server
    private Channel outboundChannel;

    public HttpRequestHandler(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final Channel inboundChannel = ctx.channel();

        // Start the connection attempt.
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new OutboundChannelInitializer(new HttpResponseHandler(inboundChannel)))
                .option(ChannelOption.AUTO_READ, false);


        ChannelFuture f = b.connect(remoteHost, remotePort);
        outboundChannel = f.channel();
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    // connection complete start to read first data
                    inboundChannel.read();
                } else {
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        if (outboundChannel.isActive()) { //if a message is received from the source, write it to the server using outbound channel
            outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        // was able to flush out data, start to read the next chunk
                        ctx.channel().read();
                    } else {
                        future.channel().close();
                    }
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
