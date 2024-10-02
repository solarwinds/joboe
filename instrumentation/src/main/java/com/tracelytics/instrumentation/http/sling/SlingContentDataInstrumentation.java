package com.tracelytics.instrumentation.http.sling;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Reports the servlet used in the Sling component processing by capturing the result of getServlet method of 
 * ContentData. Take note that this method can be invoked from outside of Sling component processing profile.
 * Therefore we check whether there is an active profile before reporting the info event
 * 
 * @author pluk
 *
 */
public class SlingContentDataInstrumentation extends SlingBaseInstrumentation {
    private static final String CLASS_NAME = SlingContentDataInstrumentation.class.getName();
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("getServlet", new String[] { }, "javax.servlet.Servlet", OpType.GET_SERVLET)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (CtMethod method : matchingMethods.keySet()) {
            if (shouldModify(cc, method)) {
                insertAfter(method, CLASS_NAME + ".reportServlet($_);", true);
            }
        }
        
        return true;
    }
    
    public static void reportServlet(Object servlet) {
        if (SlingRequestProcessorInstrumentation.hasActiveProfile() && servlet != null) { //only report an info event if there is an active sling processor profile
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "info",
                          "ActiveServlet", servlet.getClass().getName());
    
            event.report();
        }
    }

    private enum OpType {
        GET_SERVLET
    }
  
}