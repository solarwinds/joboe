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
 * Instruments request resolution within Sling - based on target resource type, request selector, request extension and request method name.  
 * 
 * @author pluk
 *
 */
public class SlingServletResolverInstrumentation extends SlingBaseInstrumentation {
    private static final String CLASS_NAME = SlingServletResolverInstrumentation.class.getName();
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("resolveServlet", new String[] {"org.apache.sling.api.SlingHttpServletRequest"}, "javax.servlet.Servlet", OpType.RESOLVE, true)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            insertBefore(matchingMethodEntry.getKey(), CLASS_NAME + ".profileEntry($1, \"" + matchingMethodEntry.getKey().getName() + "\", this);");
            insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".profileExit($_);", true);
        }
        
        return true;
    }
    
     

    public static void profileEntry(Object request, String methodName, Object resolver) {
        String resourcePath = null;
        String resourceType = null;
        
        if (request instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest)request;
            SlingResource resource = slingRequest.getTvResource();
            if (resource != null) {
                resourcePath = resource.getPath();
                resourceType = resource.getResourceType();
            }
        }
                          
                         
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry",
                      "SlingAction", "sling-resolve-servlet",
                      "FunctionName", methodName,
                      "Class", resolver.getClass().getName());

        if (resourcePath != null) {
            event.addInfo("ResourcePath", resourcePath);
        }
        if (resourceType != null) {
            event.addInfo("ResourceType", resourceType);
        }

        event.report();
    }

    public static void profileExit(Object resolvedServlet) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit");
                      
        
        if (resolvedServlet != null) {
            event.addInfo("ResolvedServlet", resolvedServlet.getClass().getName());
        }

        event.report();
    }
    
   
    private enum OpType {
        RESOLVE
    }
  
}
