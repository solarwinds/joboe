package com.tracelytics.test.httpclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;


public abstract class EndpointClient {
    
    public String getURL(String uri) {
        String responseString = "";

        // Create an instance of HttpClient.
        HttpClient client = getHttpClient();

        //     Create a method instance.
        HttpGet request = new HttpGet(uri);

        try {
            // Execute the method.
            HttpResponse response = executeRequest(client, request);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                responseString = "Method failed: " + response.getStatusLine();
                return responseString;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            response.getEntity().writeTo(baos);

            responseString = baos.toString();

        } catch (IOException e) {
            responseString = "Fatal transport error: " + e.getMessage();
            e.printStackTrace();
        } finally {
            // Release the connection.
            request.releaseConnection();
        }
        return responseString;
    }
    
    protected abstract HttpClient getHttpClient();
    
    protected HttpResponse executeRequest(HttpClient client, HttpUriRequest request) throws ClientProtocolException, IOException {
        return client.execute(request);
    }
}
