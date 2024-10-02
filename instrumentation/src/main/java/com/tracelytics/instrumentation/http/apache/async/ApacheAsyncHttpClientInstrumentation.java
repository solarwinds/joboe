package com.tracelytics.instrumentation.http.apache.async;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ContextPropagationPatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;

/**
 * Instruments `org.apache.http.nio.client.HttpAsyncClient` for the entry point of oubound apache async http calls
 * 
 * Take note that info such as URL, Http Method is captured in {@link ApacheAsyncRequestProducerInstrumentation} as those info
 * might not be available to the client yet
 * 
 * @author pluk
 *
 */
public class ApacheAsyncHttpClientInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = ApacheAsyncHttpClientInstrumentation.class.getName();
    private static String LAYER_NAME = "apache-async-http-client";
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
        new MethodMatcher<MethodType>("execute", new String[]{ "org.apache.http.nio.protocol.HttpAsyncRequestProducer", "org.apache.http.nio.protocol.HttpAsyncResponseConsumer", "org.apache.http.protocol.HttpContext", "org.apache.http.concurrent.FutureCallback"} , "java.util.concurrent.Future", MethodType.EXECUTE, true)
    );
    
    private enum MethodType {
        EXECUTE
    }
    
    private static ThreadLocal<Metadata> existingContextThreadLocal = new ThreadLocal<Metadata>();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
         
        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            insertBefore(method, CLASS_NAME + ".beforeExecute($1);", false);
            insertAfter(method, CLASS_NAME + ".afterExecute($_);", true);
        }
        
        return true;
    }
  

    public static void beforeExecute(Object producerObject) {
        Metadata existingContext = Context.getMetadata();
        if (existingContext.isSampled()) {
            Metadata clonedContext = new Metadata(existingContext);
            
            Context.setMetadata(clonedContext); //so it appears as a fork
            Event event = Context.createEvent();
    
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "IsService", Boolean.TRUE,
                          "Spec", "rsc");
            ClassInstrumentation.addBackTrace(event, 2, Module.APACHE_ASYNC_HTTP);
            
            event.setAsync();
            event.report();
            
            ContextPropagationPatcher.captureContext(producerObject);
            
            existingContextThreadLocal.set(existingContext);
        } else if (existingContext.isValid()){
            ContextPropagationPatcher.captureContext(producerObject); 
        }
    }

    public static void afterExecute(Object futureObject) {
        if (!(futureObject instanceof TvContextObjectAware)) {
            logger.warn("Future object of class [" + futureObject.getClass().getName() + " not properly patched!");
            return;
        } 
        
        ContextPropagationPatcher.captureContext(futureObject);
        Context.setMetadata(existingContextThreadLocal.get()); //set back the context before the execution call, so the execution call appears to be a fork
        existingContextThreadLocal.remove();
    }
}