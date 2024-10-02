package com.appoptics.test.pojo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;

public class TestConcurrentGet {
    private static final String urlString = "http://localhost:9000";
    private static final int RUN_COUNT = 10;
    
    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        Trace.startTrace("test-http-async").report();

        CompletableFuture[] futures = new CompletableFuture[RUN_COUNT];
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(urlString)).build();
        for (int i = 0; i < RUN_COUNT; i++) {
            futures[i] = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        }
        CompletableFuture.allOf(futures).get();
        
        System.out.println(Trace.endTrace("test-http-async"));

    }
}
