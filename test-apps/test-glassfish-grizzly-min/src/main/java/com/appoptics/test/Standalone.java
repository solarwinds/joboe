package com.appoptics.test;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class Standalone {
    //private static final URI BASE_URI = URI.create("http://localhost:8080/");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.createSimpleServer();

        // Initialize and register Jersey Servlet
        ServletHandler handler = new ServletHandler(new TestServlet());
        server.getServerConfiguration().addHttpHandler(handler, "", "/*");

        server.start();
        System.in.read();
        server.stop();

    }
}
