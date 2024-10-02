package com.tracelytics.test.httpclient;

import java.io.IOException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;


public abstract class EndpointClient {
    
    public String getURL(String uri) {
        String responseString = "";

        // Create an instance of HttpClient.
        HttpClient client = getHttpClient();

        // Create a method instance.
        GetMethod method = new GetMethod(uri);

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                                        new DefaultHttpMethodRetryHandler(3, false));

        try {
            // Execute the method.
            int statusCode = executeMethod(client, method);
            
            if (statusCode != HttpStatus.SC_OK) {
                responseString = "Method failed: " + method.getStatusLine();
                return responseString;
            }

            // Read the response body.
            byte[] responseBody = method.getResponseBody();

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            responseString = new String(responseBody);

        } catch (HttpException e) {
            responseString = "Fatal protocol violation: " + e.getMessage();
            e.printStackTrace();
        } catch (IOException e) {
            responseString = "Fatal transport error: " + e.getMessage();
            e.printStackTrace();
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
        return responseString;
    }
    
    protected abstract HttpClient getHttpClient();
    
    protected int executeMethod(HttpClient client, HttpMethod method) throws HttpException, IOException {
        return client.executeMethod(method);
    }
}
