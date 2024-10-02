package com.tracelytics.test.httpclient;

import java.io.IOException;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

public class RelativeEndpointClient extends EndpointClient {
    private boolean modifyClient;
    private HostConfiguration config;


    public RelativeEndpointClient(boolean modifyClient, String host, Integer port, String scheme) {
        this.modifyClient = modifyClient;
        
        config = new HostConfiguration();
        
        if (port != null) {
            config.setHost(host, port, scheme);
        } else {
            config.setHost(host, -1, scheme);
        }
    }
    

    @Override
    protected HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        
        if (modifyClient) {
            client.setHostConfiguration(config);
        }
        
        return client;
    }
    
    @Override
    protected int executeMethod(HttpClient client, HttpMethod method)
        throws HttpException, IOException {
        if (modifyClient) { //if client is already modified then we just use the regular call
            return super.executeMethod(client, method);
        } else {
            return client.executeMethod(config, method);
        }
    }
}
