package com.tracelytics.test.pojo;

import com.appoptics.api.ext.AppOpticsSpanProcessor;
import com.appoptics.api.ext.AppOpticsTraceConfig;
import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.trace.Span;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestJob {
    private static io.opentelemetry.trace.Tracer tracer = io.opentelemetry.OpenTelemetry.getTracerProvider().get("test", "1.0");
    private static final String SERVICE_KEY = System.getenv("APPOPTICS_SERVICE_KEY");

    public static void main(String[] args) throws InterruptedException {
        //Begin A La Carte mode (from artifact appoptics-opentelemetry-sdk-standalone)
        TracerSdkProvider tracerProvider = (TracerSdkProvider) OpenTelemetry.getTracerProvider();
        //tracerProvider.addSpanProcessor(SimpleSpansProcessor.newBuilder(new AppOpticsSdkSpanExporter()).build());  //minimum span exporter
        tracerProvider.addSpanProcessor(AppOpticsSpanProcessor.newDefaultBuilder(SERVICE_KEY).build());  //span processor, which include our span exporter and also profiling capability
        tracerProvider.updateActiveTraceConfig(AppOpticsTraceConfig.getTraceConfig()); //optional, AppOptics sampler
        //End A La Carte mode

        //Being full init (from artifact appoptics-opentelemetry-sdk)
//        AppOpticsSdkTracer.init(); //convenient method to append our sampler, span exporters etc to OT SDK
        //End full init (from artifact appoptics-opentelemetry-sdk)

        //Code below is purely OT api usage - no reference to AO code nore OT SDK

        io.opentelemetry.trace.Span span = tracer.spanBuilder("testSpan").setAttribute("ao.profile", true).startSpan();
        ExecutorService executorService = Executors.newCachedThreadPool();
        try  {
            //do some async work that triggers auto instrumentation/context propagation
            executorService.submit(() -> getURI("http://localhost:8080/greeting", span));
            executorService.submit(() -> getURI("http://localhost:8080/greeting", span));
        } finally {
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
            span.end();
        }
    }

    private static void getURI(String target, Span parentSpan) {
        Span span = tracer.spanBuilder(target).setSpanKind(Span.Kind.CLIENT).setParent(parentSpan).startSpan();
        span.setAttribute("http.method", "GET");
        span.setAttribute("http.url", target);

        try (Scope scope = tracer.withSpan(span)){
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(target))
                    .GET();

            OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), requestBuilder, new HttpTextFormat.Setter<HttpRequest.Builder>() {
                @Override
                public void set(HttpRequest.Builder requestBuilder, String key, String value) {
                    requestBuilder.setHeader(key, value);
                }
            });

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
            span.setAttribute("http.status", response.statusCode());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            span.end();
        }
    }
}
