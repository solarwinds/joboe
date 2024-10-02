package com.appoptics.test;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class StandaloneNonBlocking {
    //private static final URI BASE_URI = URI.create("http://localhost:8080/");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.createSimpleServer();

        // Initialize and register Jersey Servlet
        server.getServerConfiguration().addHttpHandler(new NonBlockingHandler(), "", "/*");

        server.start();
        System.in.read();
        server.stop();

    }
}
