package com.tracelytics.instrumentation.jcr;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments the execution of {@code javax.jcr.query.Query}
 * @author pluk
 *
 */
public class JcrQueryInstrumentation extends ClassInstrumentation {
    private static final String LAYER_NAME = "jcr";
    private static final String CLASS_NAME = JcrQueryInstrumentation.class.getName();  
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(
                                                                              new MethodMatcher<Object>("execute", new String[] { }, "javax.jcr.query.QueryResult")
                                                                          );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method,  CLASS_NAME + ".layerEntry(getLanguage(), getStatement());");
            insertAfter(method,  CLASS_NAME + ".layerExit($_);");
        }
        
        return true;
    }
    
    
    public static void layerEntry(String language, String statement) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry");
                      
        if (language != null) {
            language = language.toLowerCase();
            if (!language.startsWith("jcr-")) {
                language = "jcr-" + language;
            }
            
            event.addInfo("Flavor", language);
        }
        if (statement != null) {
            event.addInfo("Query", statement);
        }
        
        addBackTrace(event, 1, Module.JCR);
        
        event.report();
    }
    
    public static void layerExit(Object queryResult) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit");
                      
        
        event.report();
    }
}