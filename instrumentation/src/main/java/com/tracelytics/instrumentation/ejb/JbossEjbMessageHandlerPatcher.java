package com.tracelytics.instrumentation.ejb;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.http.ServletInstrumentation;

/**
 * Extracts the x-trace id in the attachments returned from EJB server 
 * @author Patson Luk
 *
 */
public class JbossEjbMessageHandlerPatcher extends ClassInstrumentation {
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(new MethodMatcher<Object>("readAttachments", 
            new String[] { "java.io.ObjectInput" }, 
            "java.util.Map"));

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertAfter(method, CLASS_NAME + ".extractXTraceIdFromAttachments($_);");
        }
        
        return true;
    }

    public static void extractXTraceIdFromAttachments(Map<String, Object> attachments) {
        if (attachments != null && 
            attachments.containsKey(ServletInstrumentation.XTRACE_HEADER) &&
            attachments.get(ServletInstrumentation.XTRACE_HEADER) instanceof String) {
            JbossEjbContext.setResponseXTraceId((String) attachments.get(ServletInstrumentation.XTRACE_HEADER));
        }
    }
    
    static String CLASS_NAME = JbossEjbMessageHandlerPatcher.class.getName();
}
