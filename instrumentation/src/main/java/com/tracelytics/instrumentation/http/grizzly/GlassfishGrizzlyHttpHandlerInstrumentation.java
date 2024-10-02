package com.tracelytics.instrumentation.http.grizzly;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.XTraceOptionsResponse;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.span.impl.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Instruments Glassfish Grizzly http server class `org.glassfish.grizzly.http.server.HttpHandler` for root span entry.
 *
 * The span exit will be taken care of by {@link GlassfishGrizzlyResponseInstrumentation }
 *
 * Take note that Glassfish Grizzly is as a default for glassfish jersey standalone mode
 */
public class GlassfishGrizzlyHttpHandlerInstrumentation extends ClassInstrumentation {

    private static String CLASS_NAME = GlassfishGrizzlyHttpHandlerInstrumentation.class.getName();
    private static String SPAN_NAME = "glassfish-grizzly";
    
    //Flag for whether hide query parameters as a part of the URL or not. By default false (do not hide)
    private static boolean hideUrlQueryParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.GRIZZLY) : false;
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(new MethodMatcher<OpType>("service", new String[] { "org.glassfish.grizzly.http.server.Request", "org.glassfish.grizzly.http.server.Response" }, "void", OpType.SERVICE));
    
    private enum OpType {
        SERVICE
    }

    private static final ThreadLocal<Integer> depthThreadLocal = new ThreadLocal<Integer>();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            insertBefore(methodEntry.getKey(), CLASS_NAME + ".handleEntry($1, $2);", false);
            addErrorReporting(methodEntry.getKey(), Throwable.class.getName(), SPAN_NAME, classPool);
            insertAfter(methodEntry.getKey(), CLASS_NAME + ".handleExit();", true, false);
        }
        return true;
    }

    public static void handleEntry(Object requestObject, Object responseObject) {
        if (!(requestObject instanceof GlassfishGrizzlyRequest)) {
            logger.warn("Glassfish Grizzly request object not properly tagged as " + GlassfishGrizzlyRequest.class.getName());
            return;
        }
        if (!(responseObject instanceof GlassfishGrizzlyResponse)) {
            logger.warn("Glassfish Grizzly response object not properly tagged as " + GlassfishGrizzlyResponse.class.getName());
            return;
        }
        final GlassfishGrizzlyRequest request = (GlassfishGrizzlyRequest) requestObject;
        final GlassfishGrizzlyResponse response = (GlassfishGrizzlyResponse) responseObject;

        if (isRootCallEntry()) {
            Map<XTraceHeader, String> xTraceHeaders = getXTraceHeaders(request);
            Tracer.SpanBuilder startTraceSpanBuilder = getStartTraceSpanBuilder(SPAN_NAME, xTraceHeaders, request.getRequestURI(), true);

            //We want an active scope as any instrumentation triggered in this thread, we would want to auto instrument
            //However, we do NOT want auto finish on span on scope closing as we want to finish the span in HttpResponse.finish instead
            //HttpResponse.finish captures both blocking and non-blocking handling
            Scope scope = startTraceSpanBuilder.startActive(false);
            Span span = scope.span();

            ClassInstrumentation.setForwardedTags(span,new HeaderExtractor<String, String>() {
                @Override public String extract(String s) {
                    return request.getHeader(s);
                }
            });

            span.setTag("Spec", "ws");
            if (request.getRequestURI() != null) {
                if (hideUrlQueryParams) {
                    span.setTag("URL", request.getRequestURI());
                } else {
                    String query = request.getQueryString();
                    span.setTag("URL", query != null && !("".equals(query)) ? (request.getRequestURI() + "?" + query) : request.getRequestURI());
                }
            }
            if (request.tvGetMethod() != null) {
                span.setTag("HTTPMethod", request.tvGetMethod());
            }

            Metadata startContext = span.context().getMetadata();
            if (startContext.isSampled()) {

                if (request.getServerName() != null) {
                    span.setTag("HTTP-Host", request.getServerName());
                }
                if (request.getScheme() != null) {
                    span.setTag("Proto", request.getScheme());
                }

                if (request.getRemoteAddr() != null) {
                    span.setTag("ClientIP", request.getRemoteAddr());
                }


                //Generate and set response Header as the headers are sent before the exit of `service` method
                Metadata exitContext = new Metadata(startContext);
                exitContext.randomizeOpID();

                response.setHeader(ClassInstrumentation.XTRACE_HEADER, exitContext.toHexString());
                span.setSpanPropertyValue(Span.SpanProperty.EXIT_XID, exitContext.toHexString());

                XTraceOptionsResponse xTraceOptionsResponse = XTraceOptionsResponse.computeResponse(span);
                if (xTraceOptionsResponse != null) {
                    response.setHeader(ClassInstrumentation.X_TRACE_OPTIONS_RESPONSE_KEY, xTraceOptionsResponse.toString());
                }
            }

            response.tvSetSpan(span); //set to the response so it can finish it later

            //set request header (both xtrace and span key) for other instrumented components (for example servlet) can continue on with x-trace id or the span even though it's not on the same thread
            request.tvSetRequestHeader(ClassInstrumentation.XTRACE_HEADER, startContext.toHexString());
            request.tvSetRequestHeader(X_SPAN_KEY, String.valueOf(SpanDictionary.setSpan(span)));
        }
    }

    /**
     * Only closes the scope here (so operations in current thread would no longer be sampled), the actual span finish
     * is implemented in {@link GlassfishGrizzlyResponseInstrumentation}
     */
    public static void handleExit() {
        if (isRootCallExit()) {
            Scope scope = ScopeManager.INSTANCE.active();
            if (scope != null) { //active scopes might have been cleared by other instrumentations (such as servlet instrumentation)
                scope.close();
            }
        }
    }


    private static boolean isRootCallEntry() {
        Integer currentDepth = depthThreadLocal.get();
        if (currentDepth == null) {
            depthThreadLocal.set(1);
            return true;
        } else {
            depthThreadLocal.set(currentDepth + 1);
            return false;
        }
    }

    private static boolean isRootCallExit() {
        Integer currentDepth = depthThreadLocal.get();
        if (currentDepth == null) {
            return false;
        } else {
            if (currentDepth == 1) {
                depthThreadLocal.remove();
                return true;
            } else {
                depthThreadLocal.set(currentDepth - 1);
                return false;
            }
        }
    }

    private static Map<XTraceHeader, String> getXTraceHeaders(GlassfishGrizzlyRequest request) {
        Map<XTraceHeader, String> xTraceHeaders = new HashMap<XTraceHeader, String>();
        for (Entry<String, XTraceHeader> headerEntry : ClassInstrumentation.XTRACE_HTTP_HEADER_KEYS.entrySet()) {
            String headerKey = headerEntry.getKey();
            String value = request.getHeader(headerKey);
            if (value != null) {
                xTraceHeaders.put(headerEntry.getValue(), value);
            }
        }
        return xTraceHeaders;
    }
}