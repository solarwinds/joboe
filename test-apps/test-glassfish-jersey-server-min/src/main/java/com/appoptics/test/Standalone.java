package com.appoptics.test;

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
        resourceConfig.registerClasses(org.glassfish.jersey.jackson.JacksonFeature.class);
        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, resourceConfig);
        System.out.println(String.format("Application started.\nTry out %s\nHit enter to stop it...",
                BASE_URI));
        System.in.read();
        httpServer.stop();
    }
}
