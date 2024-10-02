package com.tracelytics.instrumentation.http.sling;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments request processing done within Sling. Most of the Sling requests are processed using {@code SlingRequestProcessor}. We capture the {@code processRequest} method to indicate
 * the start and end of the sling layer
 * 
 * Besides we also capture {@code doProcessRequest} or {@processRequest} to provide duration of the processing on the resolved resource as profile.
 * 
 * @author pluk
 *
 */
public class SlingRequestProcessorInstrumentation extends SlingBaseInstrumentation {
    private static final String CLASS_NAME = SlingRequestProcessorInstrumentation.class.getName();
    private static final String PROFILE_NAME_UNKNOWN = "unknown-resource-type";
    private static final ThreadLocal<Boolean> hasActiveProfile = new ThreadLocal<Boolean>();
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("processRequest", new String[] {"javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse", "org.apache.sling.api.resource.ResourceResolver"}, "void", OpType.REQUEST), //Apache Sling engine 2.2.0
        new MethodMatcher<OpType>("doProcessRequest", new String[] {"javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse", "org.apache.sling.api.resource.ResourceResolver"}, "void", OpType.REQUEST), //Apache Sling engine 2.3.2
        new MethodMatcher<OpType>("processComponent", new String[] {"org.apache.sling.api.SlingHttpServletRequest", "org.apache.sling.api.SlingHttpServletResponse", "org.apache.sling.engine.impl.filter.ServletFilterManager$FilterChainType"}, "void", OpType.COMPONENT)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            if (matchingMethodEntry.getValue() == OpType.REQUEST) {
                insertBefore(matchingMethodEntry.getKey(), CLASS_NAME + ".layerEntry($1);");
                insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".layerExit();", true);
            } else if (matchingMethodEntry.getValue() == OpType.COMPONENT) {
                insertBefore(matchingMethodEntry.getKey(), CLASS_NAME + ".profileEntry($1, \"" + matchingMethodEntry.getKey().getName() + "\", this);");
                insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".profileExit($1);", true);
            }
        }
        
        
        
        return true;
    }
    
    public static void layerEntry(Object request) {
        if (shouldStartExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry");
    
            event.report();
        }
    }

    public static void layerExit() {
        if (shouldEndExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
    
            event.report();
        }
    }

    

    public static void profileEntry(Object request, String methodName, Object processor) {
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
                          
                         
        Event entryEvent = Context.createEvent();
        entryEvent.addInfo("Layer", LAYER_NAME,
                      "Label", "entry",
                      "FunctionName", methodName,
                      "Class", processor.getClass().getName());

        if (resourcePath != null) {
            entryEvent.addInfo("ResourcePath", resourcePath);
        }
        if (resourceType != null) {
            entryEvent.addInfo("ResourceType", resourceType);
        }
        
        addBackTrace(entryEvent, 1, Module.SLING);
        
        entryEvent.report();
        
        hasActiveProfile.set(true);
    }

    public static void profileExit(Object request) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit");

        event.report();
        
        hasActiveProfile.remove();
    }
    
    public static boolean hasActiveProfile() {
        if (hasActiveProfile.get() != null) {
            return hasActiveProfile.get();
        } else {
            return false;
        }
    }
   
    private enum OpType {
        REQUEST, COMPONENT
    }
  
}