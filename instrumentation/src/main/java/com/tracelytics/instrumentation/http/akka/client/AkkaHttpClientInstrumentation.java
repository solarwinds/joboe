package com.tracelytics.instrumentation.http.akka.client;

import com.tracelytics.ext.javassist.*;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.FunctionClassHelper;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.instrumentation.http.akka.server.AkkaHttpRequest;
import com.tracelytics.instrumentation.http.akka.server.AkkaHttpResponse;
import com.tracelytics.instrumentation.scala.ScalaUtil;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.TraceEventSpanReporter;
import com.tracelytics.joboe.span.impl.Tracer;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Instruments `akka.http.scaladsl.HttpExt` for outbound http request made to Akka Http Client
 *
 * Both the scala and java Akka Http Client eventually calls `akka.http.scaladsl.HttpExt`'s `singleRequest` method which
 * the remote service call entry is created and x-trace ID injected to the request
 *
 * The created span is passed to a function created by the instrumentation which such a function is added to the original `Future[HttpResponse]`
 * with the `transform` method such that the instrumentation function will be notified once the `Future` completes with a http response/exception.
 *
 * Once such a function is notified of the http response/exception, the remote service call exit is created accordingly
 *
 */
public class AkkaHttpClientInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = AkkaHttpClientInstrumentation.class.getName();
    static String LAYER_NAME = "akka-http-client";


    private static boolean hideUrlQueryParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.AKKA_HTTP) : false;
    // List of method matchers that declare method with signature patterns that we want to instrument

    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("singleRequest", new String[] { "akka.http.scaladsl.model.HttpRequest", "akka.http.scaladsl.HttpsConnectionContext", "akka.http.scaladsl.settings.ConnectionPoolSettings", "akka.event.LoggingAdapter" }, "scala.concurrent.Future", OpType.SINGLE_REQUEST),
            new MethodMatcher<OpType>("singleRequestImpl", new String[] { "akka.http.scaladsl.model.HttpRequest", "akka.http.scaladsl.HttpsConnectionContext", "akka.http.scaladsl.settings.ConnectionPoolSettings", "akka.event.LoggingAdapter" }, "scala.concurrent.Future", OpType.SINGLE_REQUEST_IMPL) //newer version has this method which should be instrumented to cover both java and scala dsl
    );

    private static Constructor<?> finishFunctionConstructor = null;

    private static final ThreadLocal<Span> activeSpanThreadLocal = new ThreadLocal<Span>();

    private enum OpType {
        SINGLE_REQUEST, SINGLE_REQUEST_IMPL
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        synchronized(AkkaHttpClientInstrumentation.class) {
            if (finishFunctionConstructor == null) {
                finishFunctionConstructor = createFinishFunctionClass();
            }
        }

        ScalaUtil.addScalaContextForInstrumentation(cc);

        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);

        boolean hasSingleRequestImplMethod = matchingMethods.values().contains(OpType.SINGLE_REQUEST_IMPL);

        for (final Map.Entry<CtMethod, OpType> methodEntry : matchingMethods.entrySet()) {
            OpType type = methodEntry.getValue();
            /*
            If there is `singleRequestImpl` method, then do not instrument `singleRequest`
            This is because in newer version of akka http client, the java dsl would call
            the `singleRequestImpl` method directly (instead of `singleRequest` as in older version).
            If we instrument both `singleRequestImpl` and `singleRequest` then requests using the scala dsl
            will be double instrumented.

            Hence our strategy here is to instrument only `singleRequestImpl` if it exists; otherwise
            instrument `singleRequest` method
            */

            if ((!hasSingleRequestImplMethod && type == OpType.SINGLE_REQUEST)
            || type == OpType.SINGLE_REQUEST_IMPL) {
                CtMethod method = methodEntry.getKey();
                    insertBefore(method,
                            "Object patchedRequest = " + CLASS_NAME + ".requestStart($1);" +
                    "if (patchedRequest != null) {" +
                    "    $1 = (akka.http.scaladsl.model.HttpRequest) patchedRequest;" +
                    "}", false);

                    insertAfter(method,
             Span.class.getName() + " activeSpan = " + CLASS_NAME + ".consumeActiveSpan();" +
                     "if (activeSpan != null) {" + //then need to handle span exit
                    "        Object finishFunction = " + CLASS_NAME + ".createFinishFunctionInstance(activeSpan);" +
                    "        if (finishFunction instanceof scala.Function1) {" +
                    "            $_.transform((scala.Function1) finishFunction, " + ScalaUtil.EXECUTION_CONTEXT_FIELD + ");" +
                    "        }" +
                    "    }"
                    , true, false);
            }
        }

        return true;
    }

    public static Object createFinishFunctionInstance(Span span) {
        if (finishFunctionConstructor != null) {
            try {
                return finishFunctionConstructor.newInstance(span);
            } catch (Exception e) {
                logger.warn("Failed to create akka http client finish function instance : " + e.getMessage());
            }
        }
        return null;
    }

    private Constructor<?> createFinishFunctionClass() throws CannotCompileException, NotFoundException, ClassNotFoundException {
        String callbackClassSimpleName = AkkaHttpClientInstrumentation.class.getSimpleName() + "FinishFunction";
        FunctionClassHelper helper = FunctionClassHelper.getInstance(classPool, "scala.runtime.AbstractFunction1", callbackClassSimpleName);

        CtClass functionClass = helper.getFunctionCtClass();
        functionClass.addField(CtField.make("private " + Span.class.getName() + " span;", functionClass));
        functionClass.addMethod(CtNewMethod.make("public Object apply(Object obj) { "
                + "scala.util.Try tryObject = (scala.util.Try) obj;"
                + CLASS_NAME + ".handleResponse(span, tryObject.isSuccess() ? tryObject.get() : ((scala.util.Failure) tryObject).exception());"
                + "span = null;"
                + "    return tryObject; "
                + "}", functionClass));
        functionClass.addConstructor(CtNewConstructor.make("public " + callbackClassSimpleName + "(" + Span.class.getName() + " span) { "
                + "this.span = span;"
                + "}", functionClass));
        return helper.toFunctionClass().getDeclaredConstructors()[0];
    }


    /**
     * Creates a span if the current context is sampled and set such a span the the Thread Local for exit point tagging
     *
     * If context is sampled, returns a request with sampled x-trace ID
     * If context valid but not sampled, returns a request with valid x-trace ID
     * If context is not valid, returns null.
     *
     * @param requestObject
     * @return
     */
    public static Object requestStart(Object requestObject) {
        if (Context.isValid()) {
            Metadata metadata;
            Span span = null;
            if (Context.getMetadata().isSampled() && requestObject instanceof AkkaHttpRequest) {
                AkkaHttpRequest request = (AkkaHttpRequest) requestObject;
                Tracer.SpanBuilder spanBuilder = Tracer.INSTANCE.buildSpan(LAYER_NAME).withReporters(TraceEventSpanReporter.REPORTER);

                spanBuilder.withTag("Spec", "rsc");
                String url = getUrl(request);
                spanBuilder.withTag("RemoteURL", url);

                if (request.tvHttpMethod() != null) {
                    spanBuilder.withTag("HTTPMethod", request.tvHttpMethod());
                }
                spanBuilder.withSpanProperty(Span.SpanProperty.IS_ASYNC, true);
                spanBuilder.withTag("IsService", true);
                span = spanBuilder.start();
                addBackTrace(span, 1, Module.AKKA_HTTP);

                metadata = span.context().getMetadata();

                //set the span to thread local so we can use it for tagging at the method exit
                activeSpanThreadLocal.set(span);
            } else { //do not create a span for request that is not sampled. Take note that we might want to create a span in the future even if it's no sampled if we need outbound metrics
                metadata = Context.getMetadata();
            }
            AkkaHttpRequest patchedRequest = ((AkkaHttpRequest) requestObject).tvWithHeader(ServletInstrumentation.XTRACE_HEADER, ((Metadata) metadata).toHexString());
            return patchedRequest;
        } else {
            return null;
        }
    }

    /**
     * Gets and removes the value from active span Thread Local. This could return null is the request was no sampled
     * @return
     */
    public static Span consumeActiveSpan() {
        Span span = activeSpanThreadLocal.get();
        activeSpanThreadLocal.remove();
        return span;
    }

    /**
     * Finishes the span with the result object when the Future is completed. Take note that the resultObject could
     * either be a `HttpResponse` if the request was processed successfully or a `Throwable` if it ended with error
     * @param span
     * @param resultObject
     */
    public static void handleResponse(Span span, Object resultObject) {
        if (span != null) {
            if (resultObject instanceof AkkaHttpResponse) {
                AkkaHttpResponse response = (AkkaHttpResponse) resultObject;
                String responseXTrace = response.tvGetHeader(ServletInstrumentation.XTRACE_HEADER);
                if (responseXTrace != null) {
                    span.setSpanPropertyValue(Span.SpanProperty.CHILD_EDGES, Collections.singleton(responseXTrace));
                }
                span.setTag("HTTPStatus", response.tvStatusCode());
            } else if (resultObject instanceof Throwable) {
                reportError(span, (Throwable) resultObject);
            }
            span.finish();
        }
    }

    private static String getUrl(AkkaHttpRequest request) {
        String fullPath;
        if (request.tvHost() != null) {
            fullPath = request.tvScheme() + "://" + request.tvHost() + (request.tvPort() != -1 ? ":" + request.tvPort() : "") + request.tvPath();
        } else { // host should not be null as akka http does not really support relative URI currently https://github.com/akka/akka/issues/20934, but just in case...
            String host = request.tvGetHeader("Host");
            fullPath = host != null ? (host + request.tvPath()) : request.tvPath();
        }

        String query = request.tvQuery();
        if (hideUrlQueryParams || query == null || query.isEmpty()) {
            return fullPath;
        } else {
            return fullPath + "?" + query;
        }
    }
}