package com.appoptics.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestService {
    
    public String processSomething(String input) {
        try {
            httpOperation("http://www.google.ca");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return "{ \"message\" : \"processed input [" + input + "]\" }";
    }
    
    public String testException(String input) {
        throw new RuntimeException("Test exception");
    }
    
    public String helloWorld() {
        return "{ \"message\" : \"hello!\" }";
    }
    
    private static void httpOperation(String target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
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
