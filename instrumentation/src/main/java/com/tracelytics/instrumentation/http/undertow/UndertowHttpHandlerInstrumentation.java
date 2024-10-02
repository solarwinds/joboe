package com.tracelytics.instrumentation.http.undertow;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
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
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.SpanDictionary;

/**
 * Instruments <code>io.undertow.server.HttpHandler</code> of Undertow. Take note that one can "chain" the handler (but the root handler should enter first and exit last). Therefore
 * we would only want to create a new trace/layer at the root handler.
 * 
 * For handlers that's chained to the root handler, they would be reported as info event instead
 * 
 * 
 * @author pluk
 *
 */
public class UndertowHttpHandlerInstrumentation extends ClassInstrumentation {

    private static String CLASS_NAME = UndertowHttpHandlerInstrumentation.class.getName();
    private static String LAYER_NAME = "undertow-http";
    
    //Flag for whether hide query parameters as a part of the URL or not. By default false (do not hide)
    private static boolean hideUrlQueryParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.UNDERTOW) : false;
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(new MethodMatcher<OpType>("handleRequest", new String[] { "io.undertow.server.HttpServerExchange" }, "void", OpType.HANDLE_REQUEST));
    
    private enum OpType {
        HANDLE_REQUEST
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            insertBefore(methodEntry.getKey(), CLASS_NAME + ".handleEntry($1, this);", false);
            addErrorReporting(methodEntry.getKey(), Throwable.class.getName(), LAYER_NAME, classPool);
        }
        return true;
    }
    public static void handleEntry(Object exchangeObject, Object handlerObject) {
        if (!(exchangeObject instanceof UndertowHttpServerExchange)) {
            logger.warn("Undertow exchange object not properly tagged as " + UndertowHttpServerExchange.class.getName());
            return;
        }
        final UndertowHttpServerExchange exchange = (UndertowHttpServerExchange) exchangeObject;
        if (!exchange.tvGetHasChecked()) { 
            exchange.tvSetHasChecked(true); //flag it so we wont check for sampling again;
            
            Map<XTraceHeader, String> xTraceHeaders = getXTraceHeaders(exchange);
            Span span = startTraceAsSpan(LAYER_NAME, xTraceHeaders, exchange.getRequestURI(), false);
            
            exchange.tvSetSpan(span);
            span.setTag("Spec", "ws");
            
            ClassInstrumentation.setForwardedTags(span, new HeaderExtractor<String, String>() {
                @Override public String extract(String s) {
                    return exchange.tvGetRequestHeader(s);
                }
            });

            if (exchange.getRequestURI() != null) {
                if (hideUrlQueryParams) {
                    span.setTag("URL", exchange.getRequestURI());
                } else {
                    String query = exchange.getQueryString();
                    span.setTag("URL", query != null && !("".equals(query)) ? (exchange.getRequestURI() + "?" + query) : exchange.getRequestURI());
                }
            }
            if (exchange.tvGetRequestMethod() != null) {
                span.setTag("HTTPMethod", exchange.tvGetRequestMethod().toString());
            }
            
            Metadata startContext = span.context().getMetadata();
            if (startContext.isSampled()) {
                span.setTag("Handler-Class", handlerObject.getClass().getName());
    
                if (exchange.getHostAndPort() != null) {
                    span.setTag("HTTP-Host", exchange.getHostAndPort());
                }
                if (exchange.getRequestScheme() != null) {
                    span.setTag("Proto", exchange.getRequestScheme());
                }
                
                String remoteClient  = exchange.tvGetRemoteClient();
                if (remoteClient != null) {
                    if (remoteClient.startsWith("/")) {
                        remoteClient = remoteClient.substring(1);
                    }
                    span.setTag("ClientIP", remoteClient);
                }

                
                //Generate and set X-Trace HTTP response Header as the headers are sent before the exit of handle method
                Metadata exitContext = new Metadata(startContext);
                exitContext.randomizeOpID();
                
                if (exchange.tvGetResponseHeader(ServletInstrumentation.XTRACE_HEADER) == null) {  //do NOT update the response header if there's already one - probably added by an earlier/parent layer
                    exchange.tvAddResponseHeader(ServletInstrumentation.XTRACE_HEADER, exitContext.toHexString());
                }
                
                //tag it to the exchange too so we can use it to create exit event
                //exchange.tvSetExitXTrace(exitContext.toHexString());
                span.setSpanPropertyValue(SpanProperty.EXIT_XID, exitContext.toHexString());
            } else { //still include the not traced x-trace ID in response header
                if (exchange.tvGetResponseHeader(ServletInstrumentation.XTRACE_HEADER) == null) {  //do NOT update the response header if there's already one - probably added by an earlier/parent layer
                    exchange.tvAddResponseHeader(ServletInstrumentation.XTRACE_HEADER, span.context().getMetadata().toHexString());
                }
            }

            XTraceOptionsResponse xTraceOptionsResponse = XTraceOptionsResponse.computeResponse(span);
            if (xTraceOptionsResponse != null) {
                exchange.tvAddResponseHeader(ServletInstrumentation.X_TRACE_OPTIONS_RESPONSE_KEY, xTraceOptionsResponse.toString());
            }
            
            //Set/update request header in case this is undertow-servlet running on this, this should be done even if request is not sampled
            exchange.tvSetRequestHeader(ServletInstrumentation.XTRACE_HEADER, startContext.toHexString());
            exchange.tvSetRequestHeader(X_SPAN_KEY, String.valueOf(SpanDictionary.setSpan(span)));
            
            Context.clearMetadata(); //clear context here as the entry instrumentation ends beyond this point. The exit point is on a different thread
        } else if (exchange.tvGetSpan() != null && ((Span) exchange.tvGetSpan()).context().isSampled()){ //the request was checked and sampled on the entry 
            //a chained handler, only report the handler class name as an info event
            Span span = (Span) exchange.tvGetSpan();
            span.log(Collections.singletonMap("Handler-Class", handlerObject.getClass().getName()));
        }
    }
    
    
    
    private static Map<XTraceHeader, String> getXTraceHeaders(UndertowHttpServerExchange exchange) {
        Map<XTraceHeader, String> xTraceHeaders = new HashMap<XTraceHeader, String>();
        for (Entry<String, XTraceHeader> headerEntry : ServletInstrumentation.XTRACE_HTTP_HEADER_KEYS.entrySet()) {
            String headerKey = headerEntry.getKey();
            String value = exchange.tvGetRequestHeader(headerKey);
            if (value != null) {
                xTraceHeaders.put(headerEntry.getValue(), value);
            }
        }
        return xTraceHeaders;
    }
}