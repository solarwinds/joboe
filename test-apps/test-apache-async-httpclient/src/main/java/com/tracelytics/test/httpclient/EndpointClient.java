package com.tracelytics.test.httpclient;

import java.io.IOException;
import java.util.concurrent.Future;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;

import com.tracelytics.test.httpclient.TestServlet.Target.Method;


public class EndpointClient {
    private final HttpAsyncClient client;
    
    private static final NByteArrayEntity entity = new NByteArrayEntity("hello".getBytes(), ContentType.TEXT_PLAIN);
    
    public EndpointClient() {
        // Create an instance of HttpAsyncClient.
        client = HttpAsyncClients.createDefault();
        ((CloseableHttpAsyncClient) client).start();

    }
    
    public Future<HttpResponse> getURL(FullUrlEndpoint endpoint) {
        //     Create a method instance.
        HttpUriRequest request = getRequest(endpoint.target.method, endpoint.target.getFullUrl());
        
        return executeRequest(client, request);
    }
    
    public Future<HttpResponse> getURL(HttpHostEndpoint endpoint) {
        String host = endpoint.target.host;
        int port = endpoint.target.port != null ? endpoint.target.port : -1;
        String uri = endpoint.target.uri;
        String scheme = endpoint.target.protocol;
        HttpHost target = new HttpHost(host, port, scheme);
        
        return executeRequest(client, getRequestProducer(endpoint.target.method, target, uri));
    }
    
    private static HttpUriRequest getRequest(Method method, String uri) {
        switch (method) {
        case GET:
            return new HttpGet(uri);
        case POST:
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(entity);
            return httpPost;
        case DELETE:
            return new HttpDelete(uri);
        case HEAD:
            return new HttpHead(uri);
        case PUT:
            HttpPut httpPut = new HttpPut(uri);
            httpPut.setEntity(entity);
            return httpPut;
        default:
            return new HttpGet(uri);
        }
    }
    
    private static HttpAsyncRequestProducer getRequestProducer(Method method, HttpHost target, String uri) {
        return HttpAsyncMethods.create(target, getRequest(method, uri));
    }
    
    
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }
    
    public void close() throws IOException {
        ((CloseableHttpAsyncClient) client).close();
    }
    
    
    
    private Future<HttpResponse> executeRequest(HttpAsyncClient client, HttpUriRequest request) {
        return client.execute(request, null);
    }
    
    private Future<HttpResponse> executeRequest(HttpAsyncClient client, HttpAsyncRequestProducer producer) {
        return client.execute(producer, HttpAsyncMethods.createConsumer(), null);
    }
}
