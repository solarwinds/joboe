package com.appoptics.test;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class StandaloneNonBlocking {
    //private static final URI BASE_URI = URI.create("http://localhost:8080/");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.createSimpleServer();

        server.getServerConfiguration().addHttpHandler(new NonBlockingHandler());

        final Map<String, String> initParams = new HashMap<String, String>();

        server.start();
        System.in.read();
        server.shutdown();

    }
}
