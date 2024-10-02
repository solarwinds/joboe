package com.tracelytics.test.httpclient;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;

public class RelativeEndpointClient extends EndpointClient {
    private boolean modifyClient;
    private String host;
    private Integer port;
    private String scheme;


    public RelativeEndpointClient(boolean modifyClient, String host, Integer port, String scheme) {
        this.modifyClient = modifyClient;
        this.host = host;
        this.port = port;
        this.scheme = scheme;
    }
    

    @Override
    protected HttpClient getHttpClient() {
     // Create an instance of HttpClient.
        HttpClient client;
        
        if (modifyClient) {
            client = new DefaultHttpClient(); //can only set client using the deprecated one from 4.3 onwards
            client.getParams().setParameter(ClientPNames.DEFAULT_HOST, port != null ? new HttpHost(host, port, scheme) : new HttpHost(host, -1 , scheme));
        } else {
            client = HttpClients.createDefault(); //use the default one otherwise
        }
        
        return client;
    }
    
        
    @Override
    protected HttpResponse executeRequest(HttpClient client, HttpUriRequest request)
        throws ClientProtocolException, IOException {
        if (!modifyClient) { //if client is not modified then we need to supply host info in the arguments
            return client.execute(port != null ? new HttpHost(host, port, scheme) : new HttpHost(host, -1, scheme), request);
        } else {
           return super.executeRequest(client, request);
        }
        
        
            
    }
      
}
