package com.appneta.test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * http/1.1 server that only talks http/1.1
 */
public class SimpleServer {
    private static final boolean SSL = System.getProperty("ssl") != null;
    private static final int ECHO_PORT = Integer.parseInt(System.getProperty("port", SSL? "8443" : "8080"));
    private static final int READ_TIMEOUT_PORT = Integer.parseInt(System.getProperty("port", SSL? "8444" : "8081"));
    private static final int WRITE_TIMEOUT_PORT = Integer.parseInt(System.getProperty("port", SSL? "8445" : "8082"));


    public static void main(String[] args) throws Exception {
        SslContext sslContext = getSslContext();

        ChannelFuture dummyChannelFuture = bindServer(ECHO_PORT, null, sslContext, new DummyServerHandler());
        ChannelFuture readTimeoutServerHandler = bindServer(READ_TIMEOUT_PORT, null, sslContext, new SlowReader());
        ChannelFuture writeTimeoutServerHandler = bindServer(WRITE_TIMEOUT_PORT, new SlowWriter(), sslContext, new DummyServerHandler());
        

        // Wait until the server socket is closed.
        // In this example, this does not happen, but you can do that to gracefully
        // shut down your server.
        dummyChannelFuture.channel().closeFuture().sync();
        readTimeoutServerHandler.channel().closeFuture().sync();
        writeTimeoutServerHandler.channel().closeFuture().sync();

    }

    private static SslContext getSslContext() throws CertificateException, SSLException {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                    .sslProvider(provider)
                    /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
                     * Please refer to the HTTP/2 specification for cipher requirements. */
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE.NO_ADVERTISE,
                            // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT.ACCEPT,
                            ApplicationProtocolNames.HTTP_1_1))
                    .build();
        } else {
            sslCtx = null;
        }
        return sslCtx;
    }

    private static ChannelFuture bindServer(int port, final ChannelHandler preHandler, final SslContext sslContext, final ChannelHandler postHandler) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        // try {
        ServerBootstrap b = new ServerBootstrap(); // (2)
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class) // (3)
                .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                System.out.println("channel " + System.identityHashCode(ch));
                                System.out.println("pipeline " + System.identityHashCode(ch.pipeline()));
                                if (sslContext != null) {
                                    ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
                                }
                                ch.pipeline().addLast(preHandler);
                                ch.pipeline().addLast(new ReadTimeoutHandler(3), new WriteTimeoutHandler(3), new HttpRequestDecoder(), new HttpResponseEncoder());
                                ch.pipeline().addLast(postHandler);
                            }
                        }).option(ChannelOption.SO_BACKLOG, 128) // (5)
                .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

        // Bind and start to accept incoming connections.
        return b.bind(port).sync(); // (7)
    }

    @Sharable
    public static class DummyServerHandler extends SimpleChannelInboundHandler<LastHttpContent> { // (1)
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, LastHttpContent lastContent) throws Exception {
            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer("dummy response".getBytes()))).addListener(
                    ChannelFutureListener.CLOSE);
        }
    }

    @Sharable
    public static class SlowReader extends SimpleChannelInboundHandler<LastHttpContent> { 
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, LastHttpContent lastContent) throws Exception {
            //trigger a timeout by doing nothing
        }
    }

    @Sharable
    public static class SlowWriter extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            //triggers a timeout by doing nothing
        }
    }
}