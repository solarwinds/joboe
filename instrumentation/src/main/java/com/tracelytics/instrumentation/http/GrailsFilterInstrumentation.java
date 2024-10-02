package com.tracelytics.instrumentation.http;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments Grails filter as profiles. Take note that we only create an entry event if the current filter accepts the request pattern (ie fulfills its regex matching criteria). This is done to avoid
 * reporting filters that do not match the current request.
 * 
 *  
 * @author pluk
 *
 */
public class GrailsFilterInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = GrailsFilterInstrumentation.class.getName();
    private static final String LAYER_NAME = "grails-filter";
    
    private static ThreadLocal<Boolean> filterSpanStarted = new ThreadLocal<Boolean>();
    private static ThreadLocal<String> currentMethodName = new ThreadLocal<String>();
           
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> handleMethodMatchers = Arrays.asList(
        new MethodMatcher<Object>("preHandle", new String[]{ "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse","java.lang.Object" }, "boolean"),
        new MethodMatcher<Object>("postHandle", new String[]{ "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse", "java.lang.Object", "org.springframework.web.servlet.ModelAndView"}, "void"),
        new MethodMatcher<Object>("afterCompletion", new String[]{ "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse", "java.lang.Object", "java.lang.Exception"}, "void")
    );
    
    private static Map<String, String> methodNamesToFilterTypes = new HashMap<String, String>(); //method names to Grails Filter type
    
    static {
        methodNamesToFilterTypes.put("preHandle", "before");
        methodNamesToFilterTypes.put("postHandle", "after");
        methodNamesToFilterTypes.put("afterCompletion", "afterView");
    }
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> acceptMethodMatchers = Arrays.asList(
        new MethodMatcher<Object>("accept", new String[]{ "java.lang.String", "java.lang.String", "java.lang.String"}, "boolean")
    );
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        try {
            cc.getDeclaredField("uriPattern");
            cc.getDeclaredField("uriExcludePattern");
            cc.getDeclaredField("controllerRegex");
            cc.getDeclaredField("actionRegex");
            cc.getDeclaredField("filterConfig");
            cc.getDeclaredField("configClass");
        } catch (NotFoundException e) {
            logger.warn("Cannot find all required fields in class [" + cc.getName() + "], skipping instrumentation");
            return false;
        }
        
        Set<CtMethod> acceptMethods = findMatchingMethods(cc, acceptMethodMatchers).keySet();
        if (acceptMethods.size() != 1) {
            logger.warn("Unexpected number of accept method in class [" + cc.getName() + "] Expected [1] Found [" + acceptMethods.size() + "]");
            return false;
        }
        
        CtMethod acceptMethod = acceptMethods.iterator().next();
        insertAfter(acceptMethod, CLASS_NAME + ".profileEntryIfAccepted($_, uriPattern, uriExcludePattern, controllerRegex, actionRegex, filterConfig, configClass);");
                
        Map<CtMethod, Object> matchingMethods = findMatchingMethods(cc, handleMethodMatchers);
        
        for (Entry<CtMethod, Object> matchingMethodEntry : matchingMethods.entrySet()) {
            insertBefore(matchingMethodEntry.getKey(), CLASS_NAME + ".markHandleStart(\"" + matchingMethodEntry.getKey().getName() + "\");");
            insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".profileExitIfAccepted();", true);
        }
        
        return true;
    }

    public static void profileEntryIfAccepted(boolean accepted, Object uriPattern, Object uriExcludePattern, Object controllerRegex, Object actionRegex, Object filterConfigObject, Object configClass) {
        //First check if the accept is invoked within an active "handle methods" (preHandle, postHandle, afterCompletion). If so and the current request match the criteria of the filter, then start a profile for it as it will be processed
        if (currentMethodName.get() != null && accepted) { 
            if (configClass != null && filterConfigObject instanceof GrailsFilterConfig) {
                Event event = Context.createEvent();
                event.addInfo("Layer", LAYER_NAME,
                              "Label", "entry",
                              "Language", "java",
                              "Class", configClass.getClass().getName());
                
                String filterType = methodNamesToFilterTypes.get(currentMethodName.get());
                if (filterType != null) {
                    event.addInfo("FilterType", filterType);
                }
                
                event.addInfo("FunctionName", ((GrailsFilterConfig)filterConfigObject).getTVConfigName());
                
                if (uriPattern != null) {
                    event.addInfo("FilterUriPattern", uriPattern.toString());
                }
                if (uriExcludePattern != null) {
                    event.addInfo("FilterUriExcludePattern", uriExcludePattern.toString());
                }
                if (controllerRegex != null) {
                    event.addInfo("FilterControllerPattern", controllerRegex.toString());
                }
                if (actionRegex != null) {
                    event.addInfo("FilterActionPattern", actionRegex.toString());
                }
                
                event.report();
                
                filterSpanStarted.set(true);
            }
        }
    }

    public static void markHandleStart(String methodName) {
        //mark the start of "handle methods" (preHandle, postHandle, afterCompletion)
        currentMethodName.set(methodName);
    }
    
    public static void profileExitIfAccepted() {
        if (filterSpanStarted.get() != null && filterSpanStarted.get()) { //then a filter span has been started, create exit event for that
            //reset the values
            filterSpanStarted.remove();
            currentMethodName.remove();
             
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
            
            event.report(); 
        }
    }
}