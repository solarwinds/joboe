package com.appoptics.test;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;


public class Standalone {
    private static final URI BASE_URI = URI.create("http://localhost:8080/");
    public static void main(String[] args) throws IOException, InterruptedException {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("com.appoptics.test");
        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, resourceConfig, false);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                httpServer.shutdownNow();
            }
        }));
        httpServer.start();

        Thread.currentThread().join();
    }
}
