package com.tracelytics.test.pojo;

import io.opentelemetry.context.Scope;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//TODO this does not work, see https://github.com/open-telemetry/opentelemetry-auto-instr-java/issues/336
public class TestJob {
    private static io.opentelemetry.trace.Tracer tracer = io.opentelemetry.OpenTelemetry.getTracerProvider().get("test", "1.0");
    public static void main(String[] args) throws InterruptedException {

        // Get the tracer : NEITHER OF BELOW WORK
        //AppOpticsSdkTracer.init(); //convenient method to append our sampler, span exporters etc to OT SDK,
        //TracerSdkProvider tracerProvider = OpenTelemetrySdk.getTracerProvider();

        //Code below is purely OT api usage - no reference to AO code nore OT SDK
        io.opentelemetry.trace.Span span = tracer.spanBuilder("testSpan").setAttribute("ao.profile", true).startSpan();
        ExecutorService executorService = Executors.newCachedThreadPool();
        try (Scope scope = tracer.withSpan(span)){
            //do some async work that triggers auto instrumentation/context propagation
            executorService.submit(() -> getURI("http://localhost:8080/greeting"));
            executorService.submit(() -> getURI("http://localhost:8080/greeting"));
        } finally {
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
            span.end();
        }
    }

    private static void getURI(String target) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(target))
                    .GET();
            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
