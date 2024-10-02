package com.tracelytics.test.pojo;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TestGet {
    private static final String urlString = "http://www.google.ca";
    
    public static void main(String[] args) throws InterruptedException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        Trace.startTrace("test-okhttp-min").report();
        ExecutorService service = Executors.newFixedThreadPool(3);
        final OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder().url(urlString).get().build();
        for (int i = 0; i < 10; i++) {
            service.submit(new Runnable() {
                public void run() {
                    try {
                        Response response = client.newCall(request).execute();
                        readResponse(response);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                }
            });
        }
        
        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
        
        CountDownLatch latch = new CountDownLatch(10);
        Callback callback = new AsyncCallback(latch);
        for (int i = 0; i < 10; i++) {
            client.newCall(request).enqueue(callback);
        }
        
        latch.await(10, TimeUnit.SECONDS);
        client.dispatcher().executorService().shutdown();
        
        Trace.endTrace("test-okhttp-min");
    }
    
    private static class AsyncCallback implements Callback {
        private final CountDownLatch latch;

        private AsyncCallback(CountDownLatch latch) {
            this.latch = latch;
        }
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            readResponse(response);
            latch.countDown();
        }
        
        @Override
        public void onFailure(Call call, IOException e) {
            e.printStackTrace();
            latch.countDown();
        }
    }
    
    private static void readResponse(Response response) throws IOException {
        int responseCode = response.code();
        String responseMessage = response.message();
        
        System.out.println(responseCode + " : " + responseMessage);
        System.out.println(new String(response.body().bytes()));
        
    }
}
