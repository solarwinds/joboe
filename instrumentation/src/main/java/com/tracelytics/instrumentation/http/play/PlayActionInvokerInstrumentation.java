package com.tracelytics.instrumentation.http.play;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;
import com.tracelytics.joboe.span.impl.SpanDictionary;
import com.tracelytics.joboe.span.impl.TraceEventSpanReporter;
import com.tracelytics.joboe.span.impl.Tracer;


/**
 * Instruments the ActionInvoker used in Play 1. This instrumentation provides
 * 1. Play layer when method "invoke" is called
 * 2. Play profile when the controller method is invoked by reflection
 * 
 * Take note that 2. takes account of the time spent in the reflection logic which is not ideal, 
 * however it is found that instrumenting the controller method directly in Play 1 triggers classloading issue as documented in
 * https://github.com/tracelytics/joboe/issues/254. Therefore we instrument the controller method from this class instead
 * 
 * 
 * @author pluk
 *
 */
public class PlayActionInvokerInstrumentation extends PlayBaseInstrumentation {

    private static String CLASS_NAME = PlayActionInvokerInstrumentation.class.getName();
    
 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("invokeControllerMethod", new String[] { "java.lang.reflect.Method", "java.lang.Object[]" }, "java.lang.Object", OpType.INVOKE_CONTROLLER),
        new MethodMatcher<OpType>("invoke", new String[] { "play.mvc.Http$Request" }, "void", OpType.INVOKE_HTTP)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
            for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
                CtMethod method = methodEntry.getKey();
                
                if (shouldModify(cc, method)) {
                    if (methodEntry.getValue() == OpType.INVOKE_CONTROLLER) { 
                        insertBefore(method, CLASS_NAME + ".profileEntry($1);", false);
                        insertAfter(method, CLASS_NAME + ".profileExit($1);", true, false);
                    } else if (methodEntry.getValue() == OpType.INVOKE_HTTP) {
                        insertBefore(method,
                                     "String spanId = null;" +
                                     "if ($1 != null && $1.headers != null && $1.headers.containsKey(\"" + X_SPAN_KEY.toLowerCase() + "\")) {" + 
                                     "    spanId = ((play.mvc.Http.Header)$1.headers.get(\"" + X_SPAN_KEY.toLowerCase() + "\")).value();" + 
                                     "}" + 
                                     CLASS_NAME + ".layerEntry(spanId);"     
                                     , false);
                        
                        addErrorReporting(method, Throwable.class.getName(), LAYER_NAME, classPool);
                        
                        insertAfter(method, CLASS_NAME + ".layerExit();", true, false);
                        
                        
                    }
                    
                }
            }
        return true;
    }
    
       
    public static void profileEntry(Method method) {
        Span span = Tracer.INSTANCE.buildSpan(LAYER_NAME + "-controller").withReporters(TraceEventSpanReporter.REPORTER).startActive().span();
        String methodDeclaringClass = method.getDeclaringClass().getName();
        String methodName = method.getName();
        span.setTag("Languauge", "java");
        span.setTag("Class", methodDeclaringClass);
        span.setTag("FunctionName", methodName);
        span.setTag("Controller", methodDeclaringClass);
        span.setTag("Action", methodName);
        
        span.setTracePropertyValue(TraceProperty.CONTROLLER, methodDeclaringClass);
        span.setTracePropertyValue(TraceProperty.ACTION, methodName);
    }

    public static void profileExit(Method method) {
        ScopeManager.INSTANCE.active().close();
    }

    public static void layerEntry(String spanId) {
        Scope parentScope = null;
        if (spanId != null) { //span ID from netty
            Span baseSpan = SpanDictionary.getSpan(Long.valueOf(spanId));
            if (baseSpan != null) {
                parentScope = ScopeManager.INSTANCE.activate((Span) baseSpan, false);
            }
        } else { //no span ID - from app container
            parentScope = ScopeManager.INSTANCE.active();
        }
        
        if (parentScope == null) {
            logger.warn("Failed to create layer + [" + LAYER_NAME + "] as parent span is not found");
            return;
        }
        
        Tracer.INSTANCE.buildSpan(LAYER_NAME).withReporters(TraceEventSpanReporter.REPORTER).asChildOf(parentScope.span()).startActive();
    }
    
    public static void layerExit() {
        ScopeManager.INSTANCE.active().close();
    }

    
    private enum OpType {
        INVOKE_CONTROLLER, INVOKE_HTTP
    }
}