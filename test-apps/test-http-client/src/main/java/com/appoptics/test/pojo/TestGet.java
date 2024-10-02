package com.appoptics.test.pojo;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestGet {
//    private static final String urlString = "http://www.google.ca";
    private static final String urlString = "http://localhost:9000";
    
    public static void main(String[] args) throws IOException, InterruptedException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        Trace.startTrace("test-http-client").report();

        HttpResponse<InputStream> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder().GET().uri(URI.create(urlString)).build(), HttpResponse.BodyHandlers.ofInputStream());
        readResponse(response);
        Trace.endTrace("test-http-client");

    }
    
    private static void readResponse(HttpResponse<InputStream> response) throws IOException {
        System.out.println(response.statusCode());
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));

        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }
}
