package com.appoptics.api;

public class TestDriver {
    public static void main(String[] args) throws InterruptedException {
//        AppOpticsSdkTracer.init();
//        io.opentelemetry.trace.Tracer tracer = io.opentelemetry.OpenTelemetry.getTracerProvider().get("test", "1.0");
//
//        io.opentelemetry.trace.Span span = tracer.spanBuilder("testSpan").startSpan();
//        ExecutorService executorService = Executors.newCachedThreadPool();
//        try (io.opentelemetry.context.Scope scope = tracer.withSpan(span)) {
//            //do some async work that triggers auto instrumentation/context propagation
//            executorService.submit(() -> getURI("http://www.google.com"));
//            executorService.submit(() -> getURI("http://www.google.ca"));
//            executorService.submit(() -> getURI("https://www.airline-club.com"));
//        } finally {
//            executorService.shutdown();
//            executorService.awaitTermination(10, TimeUnit.SECONDS);
//            span.end();
//        }
    }
//
//    private static void getURI(String target) {
//        try {
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(new URI(target))
//                    .GET()
//                    .build();
//
//            System.out.println(HttpClient.newBuilder()
//                    .build()
//                    .send(request, HttpResponse.BodyHandlers.ofString()));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
