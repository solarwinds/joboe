package com.appneta.test;

import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.example.securechat.SecureChatSslContextFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;
import org.jboss.netty.util.CharsetUtil;
import org.jboss.netty.util.HashedWheelTimer;


public class SimpleServer {
    private static final int ECHO_PORT = 8080;
    private static final int READ_TIMEOUT_PORT = 8081;
    private static final int WRITE_TIMEOUT_PORT = 8082;

    public static void main(String[] args) throws Exception {
        bindServer(ECHO_PORT, null, new DummyServerHandler());
        bindServer(READ_TIMEOUT_PORT, null, new SlowReader());
        bindServer(WRITE_TIMEOUT_PORT, new SlowWriter(), new DummyServerHandler());
    }

    private static Channel bindServer(int port, final ChannelHandler preHandler, final ChannelHandler postHandler) throws InterruptedException {
     // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
             // Create a default pipeline implementation.
                ChannelPipeline pipeline = pipeline();

                // Uncomment the following line if you want HTTPS
                SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
                engine.setUseClientMode(false);
                if (preHandler != null) {
                    pipeline.addLast("preHandler", preHandler);
                }
                
                pipeline.addLast("readTimeout", new ReadTimeoutHandler(new HashedWheelTimer(), 3));
                pipeline.addLast("writeTimeout", new WriteTimeoutHandler(new HashedWheelTimer(), 3));
                pipeline.addLast("decoder", new HttpRequestDecoder());
                pipeline.addLast("encoder", new HttpResponseEncoder());
                
                if (postHandler != null) {
                    pipeline.addLast("postHandler", postHandler);
                }
                return pipeline;
            }
            
        });

        // Bind and start to accept incoming connections.
        return bootstrap.bind(new InetSocketAddress(port));
    }
    
   
    @Sharable
    public static class DummyServerHandler extends SimpleChannelUpstreamHandler { 
        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            // Build the response object.
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
            response.setContent(ChannelBuffers.copiedBuffer("Dummy server", CharsetUtil.UTF_8));
            response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");

            // Write the response.
            ChannelFuture future = e.getChannel().write(response);
            future.addListener(ChannelFutureListener.CLOSE);
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            System.out.println("Caught exception " + e.getCause() + ". Message: " + e.getCause().getMessage() + ". Closing Channel");
            ctx.getChannel().close();
        }
    }

    @Sharable
    public static class SlowReader extends SimpleChannelUpstreamHandler { 
        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            //trigger a timeout by doing nothing
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            System.out.println("Caught exception " + e.getCause() + ". Message: " + e.getCause().getMessage() + ". Closing Channel");
            ctx.getChannel().close();
        }
    }

    @Sharable
    public static class SlowWriter extends SimpleChannelDownstreamHandler {
        @Override
        public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        }
    }
}