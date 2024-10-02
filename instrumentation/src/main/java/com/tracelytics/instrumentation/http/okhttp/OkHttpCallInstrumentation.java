package com.tracelytics.instrumentation.http.okhttp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.SpanAware;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.TraceEventSpanReporter;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.joboe.span.impl.Tracer.SpanBuilder;
import com.tracelytics.util.HttpUtils;

/**
 * Instruments `okhttp3.Call` for `execute` (synchronous) and `enqueue` (asynchronous) methods. 
 * 
 * Take note that for `enqueue` operation, this instrumentation only captures the span creation, span finishes in {@link OkHttpCallbackInstrumentation}
 *  
 * @author Patson
 *
 */

public class OkHttpCallInstrumentation extends ClassInstrumentation {
    // List of method matchers that     declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
        new MethodMatcher<MethodType>("execute", new String[] {}, "okhttp3.Response", MethodType.EXECUTE),
        new MethodMatcher<MethodType>("enqueue", new String[] { "okhttp3.Callback" }, "void", MethodType.ENQUEUE)
    );
    
    //Flag for whether hide query parameters as a part of the URL or not. By default false 
    private static boolean hideQuery = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.APACHE_HTTP) : false;
    
    private enum MethodType {
        EXECUTE, ENQUEUE
    }
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
         
        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            MethodType type = entry.getValue();
            if (type == MethodType.EXECUTE) { 
                addErrorReporting(method, Throwable.class.getName(), className, classPool);
                insertBefore(method, CLASS_NAME + ".beforeExecute(request().url() != null ? request().url().toString() : null, request().method(), request().header(\"" + ServletInstrumentation.XTRACE_HEADER + "\"));");
                insertAfter(method, CLASS_NAME + ".afterExecute($_ != null ? Integer.valueOf($_.code()) : null, $_ != null ? $_.header(\"" + ServletInstrumentation.XTRACE_HEADER + "\") : null);", true);
            } else if (type == MethodType.ENQUEUE) {
                insertBefore(method, CLASS_NAME + ".beforeEnqueue(request().url() != null ? request().url().toString() : null, request().method(), request().header(\"" + ServletInstrumentation.XTRACE_HEADER + "\"), this);");
            }
        }
        
        addSpanAware(cc);
        
        return true;
    }
    
    public static void beforeExecute(String url, String method, String generatedEntryMetadata) {
        SpanBuilder spanBuilder = Tracer.INSTANCE.buildSpan(LAYER_NAME).withReporters(TraceEventSpanReporter.REPORTER);
        spanBuilder.withTag("Spec", "rsc");
        if (url != null) {
            spanBuilder.withTag("RemoteURL", hideQuery ? HttpUtils.trimQueryParameters(url) : url);
        }
        
        if (method != null) {
            spanBuilder.withTag("HTTPMethod", method);
        }
        
        spanBuilder.withTag("IsService", true);
        
        //Since we can no longer modify the header at this point, the x-trace ID would need to be pre-generated when the call is constructed, 
        //and the generated x-trace ID would be used as the span entry x-trace ID here
        if (generatedEntryMetadata != null) { 
            try {
                Metadata entryMetadata = new Metadata(generatedEntryMetadata);
                if (entryMetadata.taskHexString().equals(Context.getMetadata().taskHexString())) {
                    spanBuilder.withSpanProperty(SpanProperty.ENTRY_XID, generatedEntryMetadata);
                } else {
                    logger.warn("Failed to create entry event of OkHttp client call with metadata [" + generatedEntryMetadata + "], the task ID is not the same as current trace");
                }
                
            } catch (OboeException e) {
                logger.warn("Failed to create entry event of OkHttp client call with metadata [" + generatedEntryMetadata + "]");
            }
        }
        
        Span span = spanBuilder.startActive().span();
        addBackTrace(span, 1, Module.OKHTTP);
    }

    public static void afterExecute(Integer statusCode, String responseXTrace) {
        Scope scope = ScopeManager.INSTANCE.active();
        if (statusCode != null) {
            scope.span().setTag("HTTPStatus", statusCode);
        }
        
        if (responseXTrace != null) {
            scope.span().setSpanPropertyValue(SpanProperty.CHILD_EDGES, Collections.singleton(responseXTrace));
        }
        
        scope.close();
    }
    
    public static void beforeEnqueue(String url, String method, String generatedEntryMetadata, Object callObject) {
        SpanBuilder spanBuilder = Tracer.INSTANCE.buildSpan(LAYER_NAME).withReporters(TraceEventSpanReporter.REPORTER);
        spanBuilder.withTag("Spec", "rsc");
        if (url != null) {
            spanBuilder.withTag("RemoteURL", hideQuery ? HttpUtils.trimQueryParameters(url) : url);
        }
        
        if (method != null) {
            spanBuilder.withTag("HTTPMethod", method);
        }
        
        spanBuilder.withTag("IsService", true);
        spanBuilder.withSpanProperty(SpanProperty.IS_ASYNC, true);
        
        //Since we can no longer modify the header at this point, the x-trace ID would need to be pre-generated when the call is constructed, 
        //and the generated x-trace ID would be used as the span entry x-trace ID here
        if (generatedEntryMetadata != null) {
            try {
                Metadata entryMetadata = new Metadata(generatedEntryMetadata);
                if (entryMetadata.taskHexString().equals(Context.getMetadata().taskHexString())) {
                    spanBuilder.withSpanProperty(SpanProperty.ENTRY_XID, generatedEntryMetadata);
                } else {
                    logger.warn("Failed to create entry event of OkHttp client call with metadata [" + generatedEntryMetadata + "], the task ID is not the same as current trace [" + Context.getMetadata().taskHexString() + "]");
                }
                
            } catch (OboeException e) {
                logger.warn("Failed to create entry event of OkHttp client call with metadata [" + generatedEntryMetadata + "]");
            }
        }
        
        Span span = spanBuilder.startManual();
        addBackTrace(span, 1, Module.OKHTTP);
        
        ((SpanAware) callObject).tvSetSpan(span);
    }
    
    public static interface Callback {
        
    }
    
    private static String CLASS_NAME = OkHttpCallInstrumentation.class.getName();
    private static String LAYER_NAME = "okhttp_client";
}