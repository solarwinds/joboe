package com.tracelytics.test.pojo;

import com.appoptics.api.ext.*;
import com.tracelytics.test.Util;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestJob {
    private static io.opentelemetry.trace.Tracer tracer = io.opentelemetry.OpenTelemetry.getTracerProvider().get("test", "1.0");
    public static void main(String[] args) throws InterruptedException {
        AppOpticsAgentSdk.init();

        AppOpticsTracerProvider tracerProvider = (AppOpticsTracerProvider) OpenTelemetry.getTracerProvider();  //ideally we don't want to overwrite the TracerProvider, but we need this now to sync scope
        tracerProvider.updateActiveTraceConfig(AppOpticsTraceConfig.getTraceConfig()); //regular OT SDK call
        tracerProvider.addSpanProcessor(AppOpticsSpanProcessor.newDefaultBuilder(null).build());  //regular OT SDK call

        io.opentelemetry.trace.Tracer tracer = io.opentelemetry.OpenTelemetry.getTracerProvider().get("test", "1.0");

        //Code below is purely OT api usage - no reference to AO code nore OT SDK
        io.opentelemetry.trace.Span span = tracer.spanBuilder("testSpan").setAttribute("ao.profile", true).startSpan();
        ExecutorService executorService = Executors.newCachedThreadPool();

        //agent instrumentation enabled
        try  (Scope scope = tracer.withSpan(span)){

            //do some async work that triggers auto instrumentation/context propagation
            executorService.submit(() -> Util.getURI("http://localhost:8080/greeting"));
            executorService.submit(() -> Util.getURI("http://localhost:8080/greeting"));
        } finally {
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
            span.end();
        }
    }


}
