package com.appoptics.test;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class Standalone {
    //private static final URI BASE_URI = URI.create("http://localhost:8080/");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.createSimpleServer();

        final Map<String, String> initParams = new HashMap<String, String>();


        // Initialize and register Jersey Servlet
        WebappContext context = new WebappContext("TestContext");
        ServletRegistration registration = context.addServlet("TestServlet", TestServlet.class);
        //registration.setInitParameter("javax.ws.rs.Application", rc.getClass().getName());
        // Add an init parameter - this could be loaded from a parameter in the constructor
        //registration.setInitParameter("myparam", "myvalue");

        registration.addMapping("");

        context.deploy(server);
        server.start();

        System.in.read();
        server.shutdown();

    }
}
