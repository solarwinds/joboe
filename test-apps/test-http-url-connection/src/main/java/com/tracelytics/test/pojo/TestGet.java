package com.tracelytics.test.pojo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.appoptics.api.ext.Trace;

public class TestGet {
    private static final String urlString = "http://www.google.ca";
    
    public static void main(String[] args) throws InterruptedException {
        Trace.startTrace("test-warmup").report();
        TimeUnit.SECONDS.sleep(2);
        Trace.endTrace("test-warmup");
        Trace.startTrace("test-http").report();
        
        ExecutorService service = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 10; i++) {
            service.submit(new Runnable() {
                public void run() {
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection)new URL(urlString).openConnection();
                        //connection.connect();
                        
                        readResponse(connection);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        
        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
        
        Trace.endTrace("test-http");
        TimeUnit.SECONDS.sleep(2);
    }
    
    private static void readResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();
        
        System.out.println(responseCode + " : " + responseMessage);
        
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            
            String line; 
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
    }
}
