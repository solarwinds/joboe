package com.tracelytics.instrumentation.http.sling;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments time taken to resolve and initialize Sling resource  
 * @author pluk
 *
 */
public class SlingRequestDataInstrumentation extends SlingBaseInstrumentation {
    private static final String CLASS_NAME = SlingRequestDataInstrumentation.class.getName();
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
          new MethodMatcher<OpType>("initResource", new String[] { "org.apache.sling.api.resource.ResourceResolver" }, "org.apache.sling.api.resource.Resource", OpType.INIT)
    );

    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            insertBefore(matchingMethodEntry.getKey(), CLASS_NAME + ".profileEntry(\"" + matchingMethodEntry.getKey().getName() + "\", this);");
            insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".profileExit($_);", true);
        }
        
        return true;
    }

    

    public static void profileEntry(String methodName, Object resolver) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry",
                      "SlingAction", "sling-init-resource",
                      "FunctionName", methodName,
                      "Class", resolver.getClass().getName());
        
                 
        event.report();
    }
 
    public static void profileExit(Object resource) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit");
                      

        if (resource instanceof SlingResource) {
            event.addInfo("ResourceType", ((SlingResource) resource).getResourceType());
            event.addInfo("ResourcePath", ((SlingResource) resource).getPath());
        }

        event.addInfo("ResourceFound", resource != null);
        event.report();
    }
    
  
    private enum OpType { INIT }
  
}