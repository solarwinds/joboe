package com.tracelytics.instrumentation.http.apache.async;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ContextPropagationPatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments the apache future to capture async exit for outbound http calls
 * 
 * Take note that `HttpAsyncResponseConsumer` should have worked too (and probably cleaner), 
 * but version 4.0 does not notify `HttpAsyncResponseConsumer` on exception
 * 
 * @author pluk
 *
 */
public class ApacheFutureInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = ApacheFutureInstrumentation.class.getName();
    private static String LAYER_NAME = "apache-async-http-client";
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
        new MethodMatcher<MethodType>("completed", new String[]{ "java.lang.Object" }, "boolean", MethodType.COMPLETED, true),
        new MethodMatcher<MethodType>("failed", new String[]{ "java.lang.Exception"}, "boolean", MethodType.FAILED, true),
        new MethodMatcher<MethodType>("cancel", new String[]{ "boolean" }, "boolean", MethodType.CANCEL, true)
    );
    
    private enum MethodType {
        COMPLETED, FAILED, CANCEL
    }
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        addTvContextObjectAware(cc);
         
        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            MethodType type = entry.getValue();
            if (type == MethodType.COMPLETED) {
                insertBefore(method, 
                        "int statusCode = -1;"
                      + "if ($1 instanceof org.apache.http.HttpResponse) {"
                      + "    statusCode = ((org.apache.http.HttpResponse) $1).getStatusLine() != null ? ((org.apache.http.HttpResponse) $1).getStatusLine().getStatusCode() : -1;"
                      + "}" 
                      + CLASS_NAME + ".completed(this, statusCode);", false);
            } else if (type == MethodType.FAILED) {
                insertBefore(method, CLASS_NAME + ".failed(this, $1);", false);
            } else if (type == MethodType.CANCEL) {
                insertBefore(method, CLASS_NAME + ".cancel(this);", false);
            }
        }
        return true;
    }
    
    public static void completed(Object futureObject, int statusCode) {
        if (ContextPropagationPatcher.restoreContext(futureObject)) {
            Event exitEvent = Context.createEvent();
            
            exitEvent.addInfo("Label", "exit",
                              "Layer", LAYER_NAME);
            
            if (statusCode != -1) {
                exitEvent.addInfo("HTTPStatus", statusCode);
            }
            
            exitEvent.report();
            
            ContextPropagationPatcher.resetContext(futureObject);
        }
    }
    
    public static void failed(Object futureObject, Exception exception) {
        if (ContextPropagationPatcher.restoreContext(futureObject)) {
            if (exception != null) {
                reportError(LAYER_NAME, exception);
            }
            
            Event exitEvent = Context.createEvent();
            exitEvent.addInfo("Label", "exit",
                              "Layer", LAYER_NAME);
            exitEvent.report();
            
            ContextPropagationPatcher.resetContext(futureObject);
        }
    }
    
    public static void cancel(Object futureObject) {
        if (ContextPropagationPatcher.restoreContext(futureObject)) {
            Event exitEvent = Context.createEvent();
            exitEvent.addInfo("Label", "exit",
                              "Layer", LAYER_NAME);
            exitEvent.report();
            
            ContextPropagationPatcher.resetContext(futureObject);
        }
    }
    
}