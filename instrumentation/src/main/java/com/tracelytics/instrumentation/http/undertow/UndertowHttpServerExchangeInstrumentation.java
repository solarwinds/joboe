package com.tracelytics.instrumentation.http.undertow;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.SpanDictionary;

/**
 * Patches the <code>io.undertow.server.HttpServerExchange</code> in order to get various http service information from the exchange object.
 * 
 * Inserts handlers to add header to request and response headers for context propagation
 * 
 * Layer exits on exchange end
 * 
 * @author pluk
 *
 */
public class UndertowHttpServerExchangeInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = UndertowHttpServerExchangeInstrumentation.class.getName();
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(new MethodMatcher<OpType>("endExchange", new String[] { }, "io.undertow.server.HttpServerExchange", OpType.END_EXCHANGE));
    //private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(new MethodMatcher<OpType>("startResponse", new String[] { }, "io.undertow.server.HttpServerExchange", OpType.START_RESPONSE));
    
    private enum OpType {
        END_EXCHANGE//, START_RESPONSE
    }
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        addSpanAware(cc);
        
        cc.addMethod(CtNewMethod.make("public String tvGetRequestHeader(String headerKey) { return (getRequestHeaders() != null) ? getRequestHeaders().getFirst(headerKey) : null; } ", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetRemoteClient() { return (getConnection() != null && getConnection().getPeerAddress() != null) ? getConnection().getPeerAddress().toString() : null; } ", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetRequestMethod() { return (getRequestMethod() != null) ? getRequestMethod().toString() : null; } ", cc));

        cc.addMethod(CtNewMethod.make("public void tvSetRequestHeader(String headerKey, String headerValue) { "
                                    + "    io.undertow.util.HeaderMap requestHeaders = getRequestHeaders();"
                                    + "    if (requestHeaders != null) { "
                                    + "        io.undertow.util.HttpString httpHeaderKey = new io.undertow.util.HttpString(headerKey);"
                                    + "        if (requestHeaders.contains(httpHeaderKey)) {" //overwrite and update the request header if there's already one
                                    + "            requestHeaders.remove(httpHeaderKey);"
                                    + "        }"
                                    + "        requestHeaders.put(httpHeaderKey, headerValue);"
                                    + "    }"
                                    + "}", cc));
        cc.addMethod(CtNewMethod.make("public void tvAddResponseHeader(String headerKey, String headerValue) { if (getResponseHeaders() != null) { getResponseHeaders().put(new io.undertow.util.HttpString(headerKey), headerValue);} } ", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetResponseHeader(String headerKey) { return getResponseHeaders() != null ? getResponseHeaders().getFirst(headerKey) : null; } ", cc));
        
        cc.addField(CtField.make("private String tvExitXTrace;", cc));
        
        cc.addMethod(CtNewMethod.make("public void tvSetExitXTrace(String exitXTrace) { tvExitXTrace = exitXTrace; } ", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetExitXTrace() { return tvExitXTrace; } ", cc));
        
        cc.addField(CtField.make("private boolean tvHasChecked;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetHasChecked(boolean hasChecked) { tvHasChecked = hasChecked; } ", cc));
        cc.addMethod(CtNewMethod.make("public boolean tvGetHasChecked() { return tvHasChecked; } ", cc));
        
        tagInterface(cc, UndertowHttpServerExchange.class.getName());
        
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertAfter(method, CLASS_NAME + ".traceExit(this, null);", true, false);
        }
        
        return true;
    }
    
    public static void traceExit(Object exchangeObject, Exception exception) {
        if (exchangeObject instanceof UndertowHttpServerExchange) {
            UndertowHttpServerExchange exchange = (UndertowHttpServerExchange) exchangeObject;
            synchronized (exchange) {
              //This might be triggered multiple times, only the first time should be reported
                Span span = (Span) exchange.tvGetSpan();
                if (span != null) {
                    //lets not report the exception for now as this could be a client abortion
//                    if (exception != null) {
//                        reportError(CLASS_NAME, exception);
//                    }
                    
                    span.setTag("Status", exchange.getResponseCode());    
                    if (exception != null && exception.getMessage() != null) {
                        span.setTag("ExceptionMessage", exception.getMessage());
                    }
                    
                    span.finish();
                    
                    Context.clearMetadata();
                    
                    exchange.tvSetSpan(null);
                    SpanDictionary.removeSpan(span);
                }
            }
        } else {
            logger.warn("undertow HttpServerExchange object not property tagged: " + exchangeObject);
        }
    }
}