package com.tracelytics.test.httpclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class HttpClientWrapper {
    public static String getURL(Endpoint endpoint) {
        if (endpoint instanceof RelativeEndpoint) {
            RelativeEndpoint relativeEndpoint = (RelativeEndpoint) endpoint;
            return getURL(relativeEndpoint.getHost(), relativeEndpoint.getPort(), endpoint.getUri());
        } else {
            return getURL(null, null, endpoint.getUri());
        }
    }

    public static String getURL(String host, Integer port, String url) {
        String ret = "";

        // Create an instance of HttpClient.
        HttpClient client = HttpClients.createDefault();
                
        
        //     Create a method instance.
        HttpGet method = new HttpGet(url);
        
        if (host != null) {
            HttpParams httpParams = new BasicHttpParams();
            httpParams.setParameter(ClientPNames.DEFAULT_HOST, port != null ? new HttpHost(host, port) : new HttpHost(host));
            method.setParams(httpParams);
        }

        try {
            // Execute the method.
            HttpResponse response = client.execute(method);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                ret = "Method failed: " + response.getStatusLine();
                return ret;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            response.getEntity().writeTo(baos);

            ret = baos.toString();

        } catch (IOException e) {
            ret = "Fatal transport error: " + e.getMessage();
            e.printStackTrace();
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
        return ret;
    }

}
