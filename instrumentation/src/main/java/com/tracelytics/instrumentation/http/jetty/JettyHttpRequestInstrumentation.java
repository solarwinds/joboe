package com.tracelytics.instrumentation.http.jetty;

import com.tracelytics.ext.javassist.*;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.SpanAware;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.Span;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

/**
 * Instruments <code>org.eclipse.jetty.client.api.Request</code> to capture synchronous and asynchronous operations from Jetty http client
 */
public class JettyHttpRequestInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = JettyHttpRequestInstrumentation.class.getName();
    private static String SPAN_NAME = "jetty-http-client";
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
        new MethodMatcher<MethodType>("send", new String[]{ } , "org.eclipse.jetty.client.api.ContentResponse", MethodType.SYNC_SEND, true),
        new MethodMatcher<MethodType>("send", new String[]{ "org.eclipse.jetty.client.api.Response$CompleteListener" } , "void", MethodType.ASYNC_SEND, true)
    );
    
    private enum MethodType {
        SYNC_SEND, ASYNC_SEND
    }

    //Flag for whether hide query parameters as a part of the URL or not. By default false
    private static boolean hideQuery = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.JETTY_HTTP_CLIENT) : false;
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
         
        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            MethodType type = entry.getValue();
            if (type == MethodType.SYNC_SEND) {
                insertBefore(method, CLASS_NAME + ".beforeSyncSend(this);", false);
                addErrorReporting(method, Exception.class.getName(), SPAN_NAME, classPool);
                insertAfter(method, CLASS_NAME + ".afterSyncSend($_ != null ? Integer.valueOf($_.getStatus()) : null, $_ != null ? $_.getHeaders().get(\"" + XTRACE_HEADER + "\") : null);", true);
            } else if (type == MethodType.ASYNC_SEND) {
                insertBefore(method, CLASS_NAME + ".beforeAsyncSend(this, $1);", false);
            }

        }
        patchMethods(cc);


        tagInterface(cc, JettyHttpRequest.class.getName());
        
        return true;
    }

    private void patchMethods(CtClass cc) throws CannotCompileException {
        cc.addMethod(CtNewMethod.make("public void tvHeader(String key, String value) { header(key, value); }", cc));

        try {
            cc.getMethod("getMethod", "()Ljava/lang/String;"); //does it have the one that returns String?
            cc.addMethod(CtNewMethod.make("public String tvGetMethod() { return getMethod(); }", cc));
        } catch (NotFoundException e) {
            try {
                cc.getMethod("getMethod", "()Lorg/eclipse/jetty/http/HttpMethod;"); //does it have the one that returns HttpMethod?
                cc.addMethod(CtNewMethod.make("public String tvGetMethod() { return getMethod().toString(); }", cc));
            } catch (NotFoundException e1) {
                logger.warn("Failed to find proper getMethod method in " + cc.getName());
                cc.addMethod(CtNewMethod.make("public String tvGetMethod() { return null; }", cc));
            }
        }
    }


    public static void beforeSyncSend(Object requestObject) {
        JettyHttpRequest request = (JettyHttpRequest) requestObject;

        Metadata currentContext = Context.getMetadata();
        if (currentContext.isSampled()) {
            Scope scope = buildTraceEventSpan(SPAN_NAME).startActive(true);
            scope.span().setTag("IsService", Boolean.TRUE).setTag("Spec", "rsc").setTag("RemoteURL", getUrl(request.getURI())).setTag("HTTPMethod", request.tvGetMethod());
            addBackTrace(scope.span(), 1, Module.JETTY_HTTP_CLIENT);

            //should use the context of the created span, this is currently the same as Context.getMetadata,
            // however it could change in the future, so it's safer to grab the span context
            currentContext = scope.span().context().getMetadata();
        }

        if (currentContext.isValid()) { //then inject header
            request.tvHeader(ClassInstrumentation.XTRACE_HEADER, currentContext.toHexString());
        }
    }

    private static String getUrl(URI uri) {
        if (hideQuery) {
            try {
                return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, uri.getFragment()).toString();
            } catch (URISyntaxException e) {
                //should not happen
                return uri.toString();
            }
        } else {
            return uri.toString();
        }
    }

    public static void afterSyncSend(Integer status, String responseXTrace) {
        Scope scope = scopeManager.active();
        if (scope != null && SPAN_NAME.equals(scope.span().getOperationName())) {
            Span span = scope.span();
            if (status != null) {
                span.setTag("HTTPStatus", status);
            }
            if (responseXTrace != null) {
                try {
                    Metadata responseMetadata = new Metadata(responseXTrace);
                    if (responseMetadata.isTaskEqual(Context.getMetadata())) {
                        span.setSpanPropertyValue(Span.SpanProperty.CHILD_EDGES, Collections.singleton(responseXTrace));
                    }
                } catch (OboeException e) {
                    logger.debug("Found invalid response x-trace ID from jetty http instrumentation : [" + responseXTrace + "]");
                }

            }
            scope.close();
        }
    }

    public static void beforeAsyncSend(Object requestObject, Object listenerObject) {
        JettyHttpRequest request = (JettyHttpRequest) requestObject;

        Metadata currentContext = Context.getMetadata();
        if (currentContext.isSampled()) {
            if (listenerObject instanceof SpanAware) {
                SpanAware listener = (SpanAware) listenerObject;

                Span span = buildTraceEventSpan(SPAN_NAME).start();
                span.setTag("IsService", Boolean.TRUE).setTag("Spec", "rsc").setTag("RemoteURL", getUrl(request.getURI())).setTag("HTTPMethod", request.tvGetMethod());
                addBackTrace(span, 1, Module.JETTY_HTTP_CLIENT);

                span.setSpanPropertyValue(Span.SpanProperty.IS_ASYNC, true);

                listener.tvSetSpan(span);
                currentContext = span.context().getMetadata(); //should use the context of the created span
            } else {
                logger.warn("Cannot instrument jetty http async request as listener object [" + listenerObject + "] is not properly tagged");
            }
        }

        if (currentContext.isValid()) { //then inject header
            request.tvHeader(ClassInstrumentation.XTRACE_HEADER, currentContext.toHexString());
        }
    }
}