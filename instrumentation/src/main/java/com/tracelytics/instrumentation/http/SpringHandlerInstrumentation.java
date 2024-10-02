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

public class SpringHandlerInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = SpringHandlerInstrumentation.class.getName();
    private static final String LAYER_NAME = "spring-handler";
    
           
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(
//        new MethodMatcher<Object>("preHandle", new String[]{ "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse","java.lang.Object" }, "boolean"),
//        new MethodMatcher<Object>("postHandle", new String[]{ "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse", "java.lang.Object", "org.springframework.web.servlet.ModelAndView"}, "void"),
          new MethodMatcher<Object>("handle", new String[]{ "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse", "java.lang.Object"}, "org.springframework.web.servlet.ModelAndView")
    );

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, Object> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, Object> matchingMethodEntry : matchingMethods.entrySet()) {
            insertBefore(matchingMethodEntry.getKey(), CLASS_NAME + ".layerEntry(this);");
            
            insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".layerExit(this);", true);
        }
        
        return true;
    }

    

    public static void layerEntry(Object handler) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry",
                      "Handler", handler.getClass().getName());
        
        event.report();
    }

    public static void layerExit(Object handler) {
  
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit",
                      "Handler", handler.getClass().getName());
        
        event.report();
    }
    
   
    
  
}