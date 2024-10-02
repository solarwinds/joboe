package com.tracelytics.instrumentation.http.spray;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.SpanDictionary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Instruments http request and response handled by Spray can. Trace starts when the <code>spray.can.server.OpenRequestComponent$DefaultOpenRequest</code> is constructed.
 * Trace ends when the "sendPart" is called with <code>HttpResponse</code> or <code>ChunkedResponseStart</code>
 * 
 * 
 * @author pluk
 *
 */
public class SprayDefaultOpenRequestInstrumentation extends ClassInstrumentation {

    private static String CLASS_NAME = SprayDefaultOpenRequestInstrumentation.class.getName();
    private static String LAYER_NAME = "spray-can";
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(new MethodMatcher<OpType>("sendPart", new String[] { "spray.http.HttpMessagePartWrapper" }, "void", OpType.SEND_PART));
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<OpType>> constructorMatchers = Arrays.asList(new ConstructorMatcher<OpType>(new String[] {"java.lang.Object", "spray.http.HttpRequest"}, OpType.CTOR));
    
    private enum OpType {
        SEND_PART, CTOR
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (CtConstructor constructor : findMatchingConstructors(cc, constructorMatchers).keySet()) {

            //start a trace when the DefaultOpenRequest is constructed
            insertBefore(constructor,
                       "if ($2 != null) { "
                     + "    spray.http.HttpRequest request = $2;"
                    //read headers for Trace View headers (X-trace etc)
                     + "    java.util.Map allHeaders = new java.util.TreeMap(String.CASE_INSENSITIVE_ORDER);"
                     + "    spray.http.HttpHeader incomingXTraceIdHeader = null;"
                     + "    String host = null;"
                     + "    String remoteHost = null;"
                     + "    if (request.headers() != null) {"
                     + "        for (int i = 0; i < request.headers().length(); i++) {"
                     + "            spray.http.HttpHeader header = (spray.http.HttpHeader)request.headers().apply(i);"
                     + "            if (header != null) {"
                     + "                allHeaders.put(header.name(), header.value());" 
                     //look up Host and Remote address headers
                     + "                if (\"" + ServletInstrumentation.XTRACE_HEADER + "\".equals(header.name())) {" // keep a reference so we can remove easier later
                     + "                    incomingXTraceIdHeader = header;"
                     + "                } else if (header instanceof spray.http.HttpHeaders.Host) {"
                     + "                    host = ((spray.http.HttpHeaders.Host)header).host();"
                     + "                } else if (header instanceof spray.http.HttpHeaders.Remote$minusAddress) {"
                     + "                    remoteHost = ((spray.http.HttpHeaders.Remote$minusAddress)header).address().toString();" 
                     + "                }"
                     + "            }"
                     + "        }"
                     + "    }"
                     + "    if (request.uri() != null && request.method() != null) {"
                     //attempt to start a trace, take note that it might not start if it's not sampled
                     + "        Object httpRequestWithSpanHeader = " + CLASS_NAME + ".startTrace(request,"
                                                                                      + "allHeaders,"
                                                                                      + "request.uri().scheme(), "
                                                                                      + "request.uri().authority() != null ? request.uri().authority().port() : 0, "
                                                                                      + "request.uri().path() != null ? request.uri().path().toString() : null, "
                                                                                      + "request.uri().query() != null ? request.uri().query().toString() : \"\", "
                                                                                      + "request.method().value(), "
                                                                                      + "host, "
                                                                                      + "remoteHost);"
                     //if this request is traced, then assign the traced request to this HttpRequest argument, the request will contain the x-trace as the header within hence would be propagated downstream
                     + "        if (httpRequestWithSpanHeader != null) {"  
                     + "            $2 = (spray.http.HttpRequest)httpRequestWithSpanHeader;"   
                     + "        }"
                     + "    }"
                     + "}", false);
        }
        
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            //end a trace when sentPart is called on HttpResponse or ChuckedResponseStart. Take note that the argument is HttpMessagePartWrapper so we need to look at the wrapped message
            insertBefore(
                    methodEntry.getKey(),
                              "spray.http.HttpRequest request = request();"
                            + "if (" + CLASS_NAME + ".shouldEndTrace(request) && $1 != null) {"
                            //get the wrapped message
                            + "    spray.http.HttpMessagePart messagePart = $1.messagePart();"
                            + "    if (messagePart instanceof spray.http.HttpResponse || messagePart instanceof spray.http.ChunkedResponseStart) {"
                            + "        String spanKey = null;"
                            //read header for the context used in the request start, we have to do this as the start and end might not be always on the same DefaultOpenRequest instance
                            + "        if (request.headers() != null) {"
                            + "            for (int i = 0; i < request.headers().length(); i++) {"
                            + "                spray.http.HttpHeader header = (spray.http.HttpHeader)request.headers().apply(i);"
                            + "                if (header != null && \""  +  X_SPAN_KEY + "\".equals(header.name())) {"
                            + "                    spanKey = header.value();"
                            + "                    break;"
                            + "                }"
                            + "            }"
                            + "        }"
                            + "        if (spanKey != null) {"
                            + "            spray.http.HttpResponse response = (messagePart instanceof spray.http.HttpResponse) ? (spray.http.HttpResponse)messagePart : ((spray.http.ChunkedResponseStart)messagePart).response(); "
                            + "            String responseContext = " + CLASS_NAME + ".endTrace(spanKey, response.status() != null ? response.status().intValue() : -1, messagePart instanceof spray.http.ChunkedResponseStart, request);"
                            //if the request ended successfully, we will need to include the x-trace id in the response header
                            + "            if (responseContext != null) {"
                            + "                 scala.collection.immutable.List newHeaders = scala.collection.immutable.$colon$colon$.MODULE$.apply(new spray.http.HttpHeaders.RawHeader(\"" + ServletInstrumentation.XTRACE_HEADER + "\", responseContext), scala.collection.immutable.Nil$.MODULE$).$colon$colon$colon(response.headers());"
                            //assign it to the first argument, take note that we cannot just modify the instance as the instance is immutable (HttpResonse, List of HttpHeaders etc)
                            + "                if ($1 instanceof spray.http.HttpResponse) {"
                            + "                    $1 = ((spray.http.HttpResponse)$1).withHeaders(newHeaders);"
                            + "                } else if ($1 instanceof spray.http.Confirmed && messagePart instanceof spray.http.ChunkedResponseStart) {"
                            + "                    $1 = ((spray.http.Confirmed)$1).copy(((spray.http.ChunkedResponseStart)messagePart).copy(response.withHeaders(newHeaders)), ((spray.http.Confirmed)$1).sentAck());"
                            + "                }"
                            + "            }"
                            + "        }"
                            + "    }"
                            + "}", false);
        }
        return true;
    }
    
    /**
     * Starts or continue a trace base on the sampling logic 
     * @param requestObject     for creating new modified instance of request object for return  
     * @param xTraceHeaders
     * @param scheme
     * @param port
     * @param path
     * @param query
     * @param httpMethod
     * @param host
     * @param remoteHost
     * @return  a new modified instance of request object with the x-trace id as request header if the trace is started or continued; otherwise returns null 
     */
    public static Object startTrace(Object requestObject, final Map<String, String> allHeaders, String scheme, int port, String path, String query, String httpMethod, String host, String remoteHost) {
        if (!(requestObject instanceof SprayHttpRequest)) {
            logger.warn(requestObject + " is not correctly tagged as " + SprayHttpRequest.class.getName());
            return null;
        }
        if (path == null) {
        	logger.warn("Cannot trace spray request as path is not found");
            return null;
        }
        String url = SprayUtil.getUrl(path, query);
        
        Map<XTraceHeader, String> xTraceHeaders = new HashMap<XTraceHeader, String>();
        for (Map.Entry<String, String> entry : allHeaders.entrySet()) {
            if (XTRACE_HTTP_HEADER_KEYS.containsKey(entry.getKey())) {
                xTraceHeaders.put(XTRACE_HTTP_HEADER_KEYS.get(entry.getKey()), entry.getValue());
            }
        }
                
        Span span = startTraceAsSpan(LAYER_NAME, xTraceHeaders, url, false);

        span.setTag("Spec", "ws");
        
        ClassInstrumentation.setForwardedTags(span, new HeaderExtractor<String, String>() {
            @Override public String extract(String s) {
                return allHeaders.get(s);
            }
        });
        
        span.setTag("URL", url);
        
    	if (path != null) {
        	span.setTag("URL", url);
        }
        if (httpMethod != null) {
        	span.setTag("HTTPMethod", httpMethod);
        }
        if (host != null) {
        	span.setTag("HTTP-Host", host + (port != -1 ? ":" + port : ""));
        }
//            if (port != -1) { //TODO Port does not appear to be parsed by ETL properly now
//                entryEvent.addInfo("Port", port); 
//            }
        if (scheme != null) {
        	span.setTag("Proto", scheme);
        }
        if (remoteHost != null) {
        	span.setTag("ClientIP", remoteHost);
        }
        
        long spanKey = SpanDictionary.setSpan(span); //set the span to the dictionary for look up later
        
        //clear the context, we only need to trace the start event here, not anything that happens within Spray-Can (we do care about Spray-routing, which is taken care of by SprayHttpServiceInstrumentation
        Context.clearMetadata(); 
        
        return ((SprayHttpRequest)requestObject).tvWithHeader(X_SPAN_KEY, String.valueOf(spanKey));
    }
    
    /**
     * Quick check on whether the trace is exiting to avoid unnecessary processing. Take note that in some edge cases (for example timeout), the instrumented
     * sendPart might get called multiple time on the same request, so we need to check the tagged request object and see whether the trace has exited
     * @param requestObject
     * @return
     */
    public static boolean shouldEndTrace(Object requestObject) {
        if (!(requestObject instanceof SprayHttpRequest)) {
            logger.warn(requestObject + " is not correctly tagged as " + SprayHttpRequest.class.getName());
            return false;
        }
        return !((SprayHttpRequest)requestObject).getTvSprayCanExitReported();
    }
    
    /**
     * Ends a trace 
     * @param context
     * @param statusCode
     * @param chunkedResponse
     * @param requestObject
     * @return the context used for the trace exit event
     */
    public static String endTrace(String spanKey, int statusCode, boolean chunkedResponse, Object requestObject) {
        if (!(requestObject instanceof SprayHttpRequest)) {
            logger.warn(requestObject + " is not correctly tagged as " + SprayHttpRequest.class.getName());
            return null;
        }
        
        Span span = (Span) SpanDictionary.getSpan(Long.valueOf(spanKey));
        if (span == null) {
        	logger.warn("Failed to look up span for spray exit with span key : " + spanKey);
            return null;
        }
        
        span.setTag("Status", statusCode);
        span.setTag("ChunkedResponse", chunkedResponse);
        span.finish();
        
        String responseContext = span.getSpanPropertyValue(SpanProperty.EXIT_XID);
        
        // cleanup
        SpanDictionary.removeSpan(Long.valueOf(spanKey));
        Context.clearMetadata();
        ((SprayHttpRequest)requestObject).setTvSprayCanExitReported(true);
        
        return responseContext;
    }

}