package com.tracelytics.test.httpclient;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;


public class AbsoluteEndpointClient extends EndpointClient {

    @Override
    protected HttpClient getHttpClient() {
        return new DefaultHttpClient();
    }

    
}
