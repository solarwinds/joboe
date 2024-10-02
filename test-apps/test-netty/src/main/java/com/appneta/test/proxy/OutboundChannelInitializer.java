package com.appneta.test.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;

public class OutboundChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final HttpResponseHandler responseHandler;

    public OutboundChannelInitializer(HttpResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();

        p.addLast(new HttpClientCodec());

        // Remove the following line if you don't want automatic content decompression.
        p.addLast(new HttpContentDecompressor());

        // Uncomment the following line if you don't want to handle HttpChunks.
//        p.addLast(new HttpResponseEncoder());
        // Remove the following line if you don't want automatic content compression.
        //p.addLast(new HttpContentCompressor());

        p.addLast(responseHandler);
    }
}