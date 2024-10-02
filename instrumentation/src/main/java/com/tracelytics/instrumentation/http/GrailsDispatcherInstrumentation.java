package com.tracelytics.instrumentation.http;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * First Grails entry point. Mark that as the start and end of the Grails extent processing
 * @author pluk
 *
 */
public class GrailsDispatcherInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = GrailsDispatcherInstrumentation.class.getName();
    private static final String LAYER_NAME = "grails";
    
           
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(
        new MethodMatcher<Object>("doDispatch", new String[]{ "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse" }, "void")
    );

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, Object> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, Object> matchingMethodEntry : matchingMethods.entrySet()) {
            insertBefore(matchingMethodEntry.getKey(), CLASS_NAME + ".layerEntry($1);");
            insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".layerExit($2);", true);
        }
        
        return true;
    }

    

    public static void layerEntry(Object request) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry");
        
        event.report();
    }

    public static void layerExit(Object response) {
  
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit");
        
        event.report();
    }
    
   
    
  
}