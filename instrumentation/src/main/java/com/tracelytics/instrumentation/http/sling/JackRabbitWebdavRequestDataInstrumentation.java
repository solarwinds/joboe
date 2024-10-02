package com.tracelytics.instrumentation.http.sling;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments the parameters used in org.apache.jackrabbit.server.util.RequestData. Take note that this class is only used by Post request it seems
 * @author pluk
 *
 */
public class JackRabbitWebdavRequestDataInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = JackRabbitWebdavRequestDataInstrumentation.class.getName();
    
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("getParameterValues", new String[] { "java.lang.String" }, "java.lang.String[]", OpType.GET_MULTI),
        new MethodMatcher<OpType>("getParameter", new String[] { "java.lang.String" }, "java.lang.String", OpType.GET_SINGLE),
        new MethodMatcher<OpType>("getFileParameters", new String[] { "java.lang.String" }, "java.io.InputStream[]", OpType.GET_FILE)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        cc.addField(CtField.make("private java.util.Set reportedParams = new java.util.HashSet();", cc));
        
        for (Entry<CtMethod, OpType> methodEntry : matchingMethods.entrySet()) {
            if (methodEntry.getValue() == OpType.GET_MULTI) {
                insertAfter(methodEntry.getKey(), CLASS_NAME + ".reportParam($1, $_, reportedParams);", true);
            } else if (methodEntry.getValue() == OpType.GET_SINGLE) {
                insertAfter(methodEntry.getKey(), CLASS_NAME + ".reportParam($1, $_, reportedParams);", true);
            } else if (methodEntry.getValue() == OpType.GET_FILE) {
                insertAfter(methodEntry.getKey(), CLASS_NAME + ".reportFileParam($1, $_, reportedParams);", true);
            }
        }
        
        return true;
    }
  

    /**
     * Report the parameter if the parameter value is not null. Take note that we do not want report
     * an event if the parameter value is null, since this indicates that the RequestData object actually
     * does not contain that parameter
     * 
     * @param paramKey
     * @param paramValue
     * @param reportedParams
     */
    public static void reportParam(String paramKey, String paramValue, Set<String> reportedParams) {
        if (paramKey != null && paramValue != null) {
            if (!reportedParams.contains(paramKey)) {
                Event event = Context.createEvent();
                event.addInfo("Label", "info");
                event.addInfo("ParamKey", paramKey);
                event.addInfo("ParamValue", paramValue);
                event.report();
                
                reportedParams.add(paramKey);
            }
        }
    }
    
    /**
     * Report the parameter if the parameter value array is not empty nor null. Take note that we do not want report
     * an event if the parameter value is empty or null, since this indicates that the RequestData object actually
     * does not contain that parameter
     * 
     * @param paramKey
     * @param paramValue
     * @param reportedParams
     */
    public static void reportParam(String paramKey, String[] paramValue, Set<String> reportedParams) {
        if (paramKey != null && paramValue != null && paramValue.length > 0) {
            if (!reportedParams.contains(paramKey)) {
                Event event = Context.createEvent();
                event.addInfo("Label", "info");
                event.addInfo("ParamKey", paramKey);
                event.addInfo("ParamValue", paramValue);
                event.report();
                
                reportedParams.add(paramKey);
            }
        }
    }
    
    /**
     * Report the file parameter if the parameter value is not null. Take note that we do not want report
     * an event if the parameter value is null, since this indicates that the RequestData object actually
     * does not contain that file parameter
     * 
     * @param paramKey
     * @param fileStream
     * @param reportedParams
     */
    public static void reportFileParam(String paramKey, Object fileStream, Set<String> reportedParams) {
        if (paramKey != null && fileStream != null) {
            if (!reportedParams.contains(paramKey)) {
                Event event = Context.createEvent();
                event.addInfo("Label", "info");
                event.addInfo("FileParamKey", paramKey);
                event.report();
                
                reportedParams.add(paramKey);
            }
        }
    }
    
    
    
    private enum OpType {
        GET_MULTI, GET_SINGLE, GET_FILE
    }
  
}