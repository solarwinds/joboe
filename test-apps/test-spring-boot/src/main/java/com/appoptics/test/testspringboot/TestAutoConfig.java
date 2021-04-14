package com.appoptics.test.testspringboot;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration;

public class TestAutoConfig {
    public static void main(String[] args) {
        OpenTelemetrySdk sdk = OpenTelemetrySdkAutoConfiguration.initialize();

//        sdk.
//
//        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build();
//
//        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
//                .setTracerProvider(sdkTracerProvider)
//                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
//                .buildAndRegisterGlobal();

        Tracer tracer =
                sdk.getTracer("instrumentation-library-name", "1.0.0");


        Span span = tracer.spanBuilder("my span").startSpan();
// put the span into the current Context
        try (Scope scope = span.makeCurrent()) {
            Thread.sleep(1000);
        } catch (Throwable t) {
            span.setStatus(StatusCode.ERROR, "Change it to your error message");
        } finally {
            span.end(); // closing the scope does not end the span, this has to be done manually
        }
    }
}
