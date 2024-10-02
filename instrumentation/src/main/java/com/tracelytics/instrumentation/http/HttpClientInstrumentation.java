package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.TraceEventSpanReporter;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.util.HttpUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 *  Instruments `java.net.http.HttpClient` to create span for outbound http calls.
 */
public class HttpClientInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = HttpClientInstrumentation.class.getName();
    private static String LAYER_NAME = "java_http_client";

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
            new MethodMatcher<MethodType>("send", new String[]{ "java.net.http.HttpRequest"}, "java.net.http.HttpResponse", MethodType.SEND),
            new MethodMatcher<MethodType>("sendAsync", new String[]{ "java.net.http.HttpRequest", "java.net.http.HttpResponse$BodyHandler", "java.net.http.HttpResponse$PushPromiseHandler", "java.util.concurrent.Executor"}, "java.util.concurrent.CompletableFuture", MethodType.SEND_ASYNC, true)
    );

    //Flag for whether hide query parameters as a part of the URL or not. By default false
    private static boolean hideQuery = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.JAVA_HTTP_CLIENT) : false;

    private enum MethodType {
        SEND, SEND_ASYNC
    }

    private static ThreadLocal<Boolean> isSyncThreadLocal = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    private static ThreadLocal<Span> spanThreadLocal = new ThreadLocal<Span>();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        CtClass throwableClass = classPool.get(Throwable.class.getName());
        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            MethodType type = entry.getValue();

            String catchCode = wrapWithCatch(CLASS_NAME + ".onThrowable($e);");

            if (type == MethodType.SEND) {
                insertBefore(method, HttpClientInstrumentation.CLASS_NAME + ".sendEntry($1);", false);
                method.addCatch("{ " + catchCode + " throw $e; }", throwableClass);
                insertAfter(method, HttpClientInstrumentation.CLASS_NAME + ".sendExit($_, $1);", true, false);
            } else if (type == MethodType.SEND_ASYNC) {
                insertBefore(method, HttpClientInstrumentation.CLASS_NAME + ".sendAsyncEntry($1);", false);
                method.addCatch("{ " + catchCode + " throw $e; }", throwableClass);
                insertAfter(method, HttpClientInstrumentation.CLASS_NAME + ".sendAsyncExit($_, $1);", true, false);

            }
        }

        return true;
    }

    public static void sendEntry(Object httpRequestObject) {
        isSyncThreadLocal.set(true);
        onEntry(httpRequestObject); //entry handling is the same as async
    }

    public static void sendExit(Object responseObject, Object requestObject) {
        if (Context.getMetadata().isValid()) {
            final Span span = spanThreadLocal.get();
            if (requestObject instanceof HttpRequest) {
                ((HttpRequest) requestObject).setTvContext(null); //clear it as the request object can be reused
            }

            if (span != null) { //could be null if send is called via Facade first then the actual implementation, then it will exit on the implementation
                span.setTag("HTTPStatus", ((HttpResponse) responseObject).statusCode());
                String responseXTrace = ((HttpResponse) responseObject).tvGetHeader(XTRACE_HEADER);

                if (responseXTrace != null) {
                    span.setSpanPropertyValue(Span.SpanProperty.CHILD_EDGES, Collections.singleton(responseXTrace));
                }

                span.finish();
            }
        }
        spanThreadLocal.remove();
        isSyncThreadLocal.set(false);
    }

    public static void sendAsyncEntry(Object httpRequestObject) {
        if (!isSyncThreadLocal.get()) { //make sure we do not double entry on send (synchronous) as it eventually calls sendAsync
            onEntry(httpRequestObject);
        }
    }

    private static void onEntry(Object httpRequestObject) {
        //spanThreadLocal could be non null if it goes through a Facade first than the actual implementation, we do not want to double instrument
        if (Context.getMetadata().isValid() && spanThreadLocal.get() == null) {
            if (!(httpRequestObject instanceof HttpRequest)) {
                logger.warn(httpRequestObject + " is not tagged with " + HttpRequest.class.getName() + " . Skipping instrumentation.");
                return;
            }

            Span span;
            HttpRequest request = (HttpRequest) httpRequestObject;
            if (Context.getMetadata().isSampled()) {
                URI uri = request.uri();

                String path = uri.getPath();

                if (path == null) {
                    path = "/";
                }

                String query = uri.getQuery();
                if (query != null && !hideQuery) {
                    path += "?" + query;
                }

                String remoteUrl;
                String host = uri.getHost();
                String protocol = uri.getScheme();
                int port = uri.getPort();
                if (host != null && protocol != null) {
                    remoteUrl = protocol + "://" + (port != -1 ? (host + ":" + port) : host) + path;
                } else { //use the URI directly
                    remoteUrl = hideQuery ? HttpUtils.trimQueryParameters(uri.toString()) : uri.toString();
                }

                Tracer.SpanBuilder spanBuilder = Tracer.INSTANCE.buildSpan(LAYER_NAME)
                        .withReporters(TraceEventSpanReporter.REPORTER)
                        .withTag("Spec", "rsc")
                        .withTag("IsService", true)
                        .withTag("HTTPMethod", request.method())
                        .withTag("RemoteURL", remoteUrl);

                if (!isSyncThreadLocal.get()) {
                    spanBuilder.withSpanProperty(Span.SpanProperty.IS_ASYNC, true);
                }

                span = spanBuilder.start();

                addBackTrace(span, 1, Module.JAVA_HTTP_CLIENT);
            } else { //not sampled, still need to inject the x-trace context, create a simple span without any reporter
                span = Tracer.INSTANCE.buildSpan(LAYER_NAME).start();
            }
            request.setTvContext(span.context().getMetadata()); //tag the x-trace ID to the request used, so we can inject it to header

            //sets to thread local, such that the reference can be passed to the function that exits the span
            spanThreadLocal.set(span);
        }

    }

    public static <T> void sendAsyncExit(CompletableFuture<T> responseFuture, Object requestObject) {
        final Span span = spanThreadLocal.get();
        if (Context.getMetadata().isValid() && !isSyncThreadLocal.get() && span != null) {
            spanThreadLocal.remove();
            if (requestObject instanceof HttpRequest) {
                ((HttpRequest) requestObject).setTvContext(null); //clear it as the request object can be reused
            }
            // Returns a new `CompletableFuture` which first executes our span exit handling
            responseFuture.whenComplete(new BiConsumer<T, Throwable>() {
                @Override
                public void accept(T response, Throwable throwable) {
                    if (throwable != null) {
                        reportError(span, throwable);
                    } else {
                        span.setTag("HTTPStatus", ((HttpResponse) response).statusCode());

                        String responseXTrace = ((HttpResponse) response).tvGetHeader(XTRACE_HEADER);

                        if (responseXTrace != null) {
                            span.setSpanPropertyValue(Span.SpanProperty.CHILD_EDGES, Collections.singleton(responseXTrace));
                        }
                    }
                    span.finish();
               }
            });
        }
    }

    public static void onThrowable(Throwable e) {
        if (Context.getMetadata().isValid()) {
            Span span = spanThreadLocal.get();
            if (span != null) {
                reportError(span, e);
                span.finish();
                spanThreadLocal.remove();
            }
        }
    }
}