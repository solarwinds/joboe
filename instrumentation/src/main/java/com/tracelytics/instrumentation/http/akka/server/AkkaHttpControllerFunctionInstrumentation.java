package com.tracelytics.instrumentation.http.akka.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.*;
import com.tracelytics.ext.javassist.expr.ExprEditor;
import com.tracelytics.ext.javassist.expr.MethodCall;
import com.tracelytics.instrumentation.*;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.XTraceOptionsResponse;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.SpanDictionary;
import com.tracelytics.joboe.span.impl.Tracer;


/**
 * Instruments the Http request/response handled by Akka Http Low level API within the <code>akka.http.impl.engine.server.HttpServerBluePrint</code>
 *
 * Span entry and exit captured at the "ControllerStage":
 *
 * For upstream (incoming request), we capture the `RequestStart` handling of `InHandler` defined in "ControllerStage"
 *
 * For downstream (outgoing response), we capture the `HttpResposne` handling of `InHandler` defined in "ControllerStage"
 *
 * This also injects the span id in the request header such that other layers can continue the trace by restoring the corresponding span properly.
 *
 * For example Play MVC - since Play 2.4, there has been experimental code that uses Akka-http instead of Netty. And since Play 2.6, Akka-http became the default
 *
 * @author pluk
 *
 */
public class AkkaHttpControllerFunctionInstrumentation extends ClassInstrumentation {

    private static String CLASS_NAME = AkkaHttpControllerFunctionInstrumentation.class.getName();
    static String LAYER_NAME = "akka-http-server";


    private static boolean hideUrlQueryParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.AKKA_HTTP) : false;
    // List of method matchers that declare method with signature patterns that we want to instrument

    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("onPush", new String[] {}, "void", OpType.ON_PUSH),
            new MethodMatcher<OpType>("onUpstreamFailure", new String[] { "java.lang.Throwable" }, "void", OpType.ON_UPSTREAM_FAILURE)
    );

    private enum OpType {
        ON_PUSH, ON_UPSTREAM_FAILURE
    }

    /**
     *  Within the inHandler of the Controller stage, the onPush method calls grab(), which returns the object pushed down from the previous stage/pipeline. Therefore we
     *  will attempt to capture the grab() calls within Controller's stage's inHandler by wrapping after the grab method call
     */
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        //be very careful with the injected call, verify that $_outer is available
        try {
            cc.getDeclaredField("$outer");
        } catch (NotFoundException e) {
            logger.warn("$outer field is not found within Akka Http ControllerStage InHandler : " + cc.getName() + ", skipping instrumentation on Akka Http", e);
            return false;
        }

        cc.addField(CtField.make("private Object tvActiveSpan;", cc));

        for (final Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            OpType type = methodEntry.getValue();
            CtMethod method = methodEntry.getKey();
            if (type == OpType.ON_PUSH) {
                insertBefore(method, "tvActiveSpan = " + CLASS_NAME + ".getSpanFromRequestAwareObject($outer);", false);
                method.instrument(new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {

                        // Looking for schemeField = iprot.readFieldBegin();
                        if (m.getMethodName().equals("grab")) {
                            insertAfterMethodCall(m,
                                    "java.lang.Object modifiedObject = " + CLASS_NAME + ".handleGrab($_, tvActiveSpan);"
                                            + "if (modifiedObject != null) { $_ = modifiedObject; }", false);
                        }
                    }
                });
                insertAfter(method, "tvActiveSpan = null;", true, false);
            } else if (type == OpType.ON_UPSTREAM_FAILURE) {
                insertBefore(methodEntry.getKey(), CLASS_NAME + ".reportException($1, $outer);", false);
            }
        }

        return true;
    }

    public static Object handleGrab(Object grabbedObject, Object activeSpanObject) {
        try {
            if (grabbedObject instanceof AkkaHttpRequestStart) {
                return handleRequestStart((AkkaHttpRequestStart) grabbedObject);
            } else if (grabbedObject instanceof AkkaHttpResponse) {
                return handleResponse((AkkaHttpResponse) grabbedObject, (Span) activeSpanObject);
            }
        } catch (Throwable e) { //have to catch it here due to https://issues.jboss.org/browse/JASSIST-210
            logger.warn("Caught exception as below. Please take note that existing code flow should not be affected, this might only impact the instrumentation of current trace");
            logger.warn(e.getMessage(), e);
        }

        return null;
    }

    private static AkkaHttpRequestStart handleRequestStart(final AkkaHttpRequestStart requestStart) {
        Map<XTraceHeader, String> xTraceHeaders = new HashMap<XTraceHeader, String>();
        for (Entry<String, XTraceHeader> headerKeyEntry : ServletInstrumentation.XTRACE_HTTP_HEADER_KEYS.entrySet()) {
            String httpKey = headerKeyEntry.getKey();
            String headerValue = requestStart.tvGetHeader(httpKey);
            if (headerValue != null) {
                xTraceHeaders.put(headerKeyEntry.getValue(), headerValue);
            }
        }

        String url = getUrl(requestStart.tvUriPath(), requestStart.tvQuery());
        Tracer.SpanBuilder spanBuilder = getStartTraceSpanBuilder(LAYER_NAME, xTraceHeaders, url);

        if (requestStart.tvUriPath() != null) {
            spanBuilder.withTag("URL", getUrl(requestStart.tvUriPath(), requestStart.tvQuery()));
        }

        String host = requestStart.tvUriHost();
        if (host != null) {
            int port = requestStart.tvUriPort();
            spanBuilder.withTag("HTTP-Host", port != 0 ? host + ":" + port : host);
        }

        if (requestStart.tvHttpMethod() != null) {
            spanBuilder.withTag("HTTPMethod", requestStart.tvHttpMethod());
        }


        if (requestStart.tvScheme() != null) {
            spanBuilder.withTag("Proto", requestStart.tvScheme());
        }

        spanBuilder.withTag("Spec", "ws");

        Span span = spanBuilder.start();

        ClassInstrumentation.setForwardedTags(span, new HeaderExtractor<String, String>() {
            @Override public String extract(String s) {
                return requestStart.tvGetHeader(s);
            }
        });
        
        long spanKey = SpanDictionary.setSpan(span);

        Metadata startContext = span.context().getMetadata();

        //set it to current context so context will get carried by Akka actor instrumentation,
        //the context will be cleared once the actor finishes processing the envelope of this task
        Context.setMetadata(startContext);

        //provide both the x-trace id and span key such that instrumentation in next layer could continue by using either of those
        return requestStart.tvWithHeader(ServletInstrumentation.XTRACE_HEADER, startContext.toHexString()).tvWithHeader(ServletInstrumentation.X_SPAN_KEY, String.valueOf(spanKey));
    }

    private static Object handleResponse(AkkaHttpResponse response, Span span) {
        if (span != null) {
            span.setTag("Status", response.tvStatusCode());
            span.finish();

            AkkaHttpResponse modifiedResponse = response.tvAddHeader(ServletInstrumentation.XTRACE_HEADER, span.getSpanPropertyValue(Span.SpanProperty.EXIT_XID));
            XTraceOptionsResponse xTraceOptionsResponse = XTraceOptionsResponse.computeResponse(span);
            if (xTraceOptionsResponse != null) {
                modifiedResponse = modifiedResponse.tvAddHeader(ServletInstrumentation.X_TRACE_OPTIONS_RESPONSE_KEY, xTraceOptionsResponse.toString());
            }

            // cleanup
            Context.clearMetadata();
            SpanDictionary.removeSpan(span);

            return modifiedResponse;
        }
        return null;
    }

    public static void reportException(Throwable throwable, Object currentRequestAwareObject) {
        if (currentRequestAwareObject instanceof AkkaHttpCurrentRequestAware) {
            Span span = getSpanFromRequestAwareObject((AkkaHttpCurrentRequestAware) currentRequestAwareObject);
            if (span != null && throwable != null) {
                reportError(span, throwable);
            }
        }
    }

    private static String getUrl(String path, String query) {
        if (hideUrlQueryParams || query == null || query.isEmpty()) {
            return path;
        } else {
            return path + "?" + query;
        }
    }

    public static Span getSpanFromRequestAwareObject(Object requestAwareObject) {
        if (!(requestAwareObject instanceof AkkaHttpCurrentRequestAware)) {
            logger.warn("Akka Http Controller instance not tagged properly : " + requestAwareObject);
            return null;
        }
        AkkaHttpRequestStart currentRequest = ((AkkaHttpCurrentRequestAware) requestAwareObject).tvGetCurrentRequest();
        if (currentRequest != null) {
            String spanKey = currentRequest.tvGetHeader(ServletInstrumentation.X_SPAN_KEY);
            if (spanKey != null) {
                return SpanDictionary.getSpan(Long.parseLong(spanKey));
            }
        }

        return null;
    }
}