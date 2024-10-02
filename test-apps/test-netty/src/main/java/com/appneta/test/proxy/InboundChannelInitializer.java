package com.appneta.test.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Initializes channel between the source (the one that sends http request) to this proxy
 */
public class InboundChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final String remoteHost;
    private final int remotePort;

    public InboundChannelInitializer(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
//        pipeline.addLast(new HttpRequestDecoder());
//        // Uncomment the following line if you don't want to handle HttpChunks.
//        pipeline.addLast(new HttpObjectAggregator(1048576));
        pipeline.addLast(new HttpServerCodec());

        // Remove the following line if you don't want automatic content decompression.
        //pipeline.addLast(new HttpContentDecompressor());

        pipeline.addLast(new LoggingHandler(LogLevel.INFO), new HttpRequestHandler(remoteHost, remotePort));
    }
}