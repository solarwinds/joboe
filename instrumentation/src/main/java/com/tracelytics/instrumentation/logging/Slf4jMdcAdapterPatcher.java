package com.tracelytics.instrumentation.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.LogTraceIdScope;

/**
 * Patches `ch.qos.logback.classic.uti.LogbackMDCAdaptor` to return ao.traceId for getter methods
 *   
 * @author pluk
 *
 */
public class Slf4jMdcAdapterPatcher extends BaseMdcPatcher {
    private static String CLASS_NAME = Slf4jMdcAdapterPatcher.class.getName();
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("get", new String[] { "java.lang.String" }, "java.lang.String", OpType.GET),
                    new MethodMatcher<OpType>("getCopyOfContextMap", new String[] { }, "java.util.Map", OpType.GET_CONTEXT),
                    new MethodMatcher<OpType>("getPropertyMap", new String[] { }, "java.util.Map", OpType.GET_PROPERTY_MAP)); //for ch.qos.logback.classic.util.LogbackMDCAdapter

    private enum OpType {
        GET, GET_CONTEXT, GET_PROPERTY_MAP;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        if (scope == LogTraceIdScope.DISABLED) {
            return false;
        }
        
        if ("org.slf4j.helpers.NOPMDCAdapter".equals(cc.getName())) { //do not modify the no-op adapter
            return false;
        }
        
        for (Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            if (entry.getValue() == OpType.GET) {
              //insert after here as some implementation keeps track of the operation (ch.qos.logback.classic.util.LogbackMDCAdapter) so we do not want to shortcut out with insertBefore
                insertAfter(method, "if (\"" + TRACE_ID_KEY + "\".equals($1)) { $_ = " + CLASS_NAME + ".getLogTraceId(); }", true, false);  
            } else if (entry.getValue() == OpType.GET_PROPERTY_MAP || entry.getValue() == OpType.GET_CONTEXT) {
                insertAfter(method, "$_ = " + CLASS_NAME + ".slf4jInsertLogTraceId($_);", true, false);
            }
        }
    	return true;
    }
    
    public static Map<String, String> slf4jInsertLogTraceId(Map<String, String> result) {
        Metadata metadata = Context.getMetadata();
        if (scope == LogTraceIdScope.ENABLED || (scope == LogTraceIdScope.SAMPLED_ONLY && metadata.isSampled())) {
            if (result == null) {
                result = Collections.synchronizedMap(new HashMap<String, String>());
            }
            result.put(TRACE_ID_KEY, metadata.getCompactTraceId());
        }
        return result;
    }
}



    