package com.tracelytics.instrumentation.http.ws;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Instruments Asynchronous operation of Spring Resttemplate. Take note that the async exit is reported by {@link RestTemplateCallbackRegistryInstrumentation}
 * 
 *    
 * @author pluk
 *
 */
public class RestTemplateAsyncRequestInstrumentation extends BaseWsClientInstrumentation {
    
    private static String LAYER_NAME = "rest_client_spring";

    private static String CLASS_NAME = RestTemplateAsyncRequestInstrumentation.class.getName();
    
    private static ThreadLocal<Metadata> contextThreadLocal = new ThreadLocal<Metadata>();
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(new MethodMatcher<Object>("executeAsync", new String[] { }, "org.springframework.util.concurrent.ListenableFuture"));
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        for (CtMethod executeAsyncMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(executeAsyncMethod, CLASS_NAME + ".doExecuteEntry(getURI() != null ? getURI().toString() : null, getMethod() != null ? getMethod().name() : null, getHeaders());", false);
            insertAfter(executeAsyncMethod, CLASS_NAME + ".doExecuteExit();", true);
        }
        return true;
    }
    
    public static void doExecuteExit() {
        contextThreadLocal.remove();
    }
    
    public static void doExecuteEntry(String uri, String httpMethod, Map<String, List<String>> headers) {
        if (Context.getMetadata().isSampled()) {
            Metadata asyncContext = layerEntryRest(httpMethod, uri, LAYER_NAME, true);
            
            if (headers != null) {
                headers.put(ServletInstrumentation.XTRACE_HEADER, Collections.singletonList(asyncContext.toHexString()));
            }
            contextThreadLocal.set(asyncContext);
        } else if (Context.getMetadata().isValid()) { //valid but not traced
            if (headers != null) {
                headers.put(ServletInstrumentation.XTRACE_HEADER, Collections.singletonList(Context.getMetadata().toHexString()));
            }
        }
        
    }
    
    /**
     * Gets active context, if there an ongoing resttemplate trace (a start event is sent)
     * @return
     */
    static Metadata getActiveContext() {
        return contextThreadLocal.get();
    }
}