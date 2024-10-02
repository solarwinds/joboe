package com.tracelytics.test.httpclient;

import org.apache.commons.httpclient.HttpClient;

public class AbsoluteEndpointClient extends EndpointClient {

    @Override
    protected HttpClient getHttpClient() {
        return new HttpClient();        
    }

    
}
