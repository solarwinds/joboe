package com.tracelytics.test.httpclient;

import com.tracelytics.test.httpclient.Target.Method;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;


public class EndpointClient implements Closeable {
    private final HttpClient client;
    
    private static final String TEST_PAYLOAD = "{ \"hello\" : true }";
    
    public EndpointClient() throws Exception {
        // Create an instance of HttpAsyncClient.
        client = new HttpClient(new SslContextFactory.Client());
        client.start();
    }

    public ContentResponse execute(Method method, String uri) {
        Request request = buildRequest(method, uri);
        try {
            return request.send();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

//    public Future<HttpResponse> process(HttpHostEndpoint endpoint) {
//        String host = endpoint.target.host;
//        int port = endpoint.target.port != null ? endpoint.target.port : -1;
//        String uri = endpoint.target.uri;
//        String scheme = endpoint.target.protocol;
//        HttpHost target = new HttpHost(host, port, scheme);
//
//        return executeRequest(client, getRequestProducer(endpoint.target.method, target, uri));
//    }
    
    public Future<ResultWithContents> asyncExecute(Method method, String uri) {
        //     Create a method instance.
        Request request = buildRequest(method, uri);
        CompletableFuture<ResultWithContents> responseFuture = new CompletableFuture<>();
        request.send(new Response.Listener.Adapter() {
            private List<ByteBuffer> buffers = new ArrayList<>();


            @Override
            public void onContent(Response response, ByteBuffer content) {
                buffers.add(content);
            }

            @Override
            public void onComplete(Result result) {
                responseFuture.complete(new ResultWithContents(result, buffers));
            }
        });
        return responseFuture;
    }

//    public Future<HttpResponse> asyncExecute(HttpHostEndpoint endpoint) {
//
//        HttpHost target = new HttpHost(host, port, scheme);
//
//        return executeRequest(client, getRequestProducer(endpoint.target.method, target, uri));
//    }


    private Request buildRequest(Method method, String uri) {
        Request request = client.newRequest(uri);
        request.method(method.name());
        switch (method) {
            case POST:
            case PUT:
                request.content(new StringContentProvider(TEST_PAYLOAD), "application/json");
                break;
        }

        return request;
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }
    
    public void close() {
        try {
            client.stop();
        } catch (Exception e) {
            //it's fine
        }
    }

    public static class ResultWithContents {
        private final Result result;
        private final ByteBuffer contents;

        private ResultWithContents(Result result, List<ByteBuffer> contents) {
            this.result = result;
            int contentsLength = 0;
            for (ByteBuffer content : contents) {
                contentsLength += content.limit();
            }

            this.contents = ByteBuffer.allocate(contentsLength);


            for (ByteBuffer content : contents) {
                this.contents.put(content);
            }
        }

        public Result getResult() {
            return result;
        }

        public ByteBuffer getContents() {
            return contents;
        }
    }

}
