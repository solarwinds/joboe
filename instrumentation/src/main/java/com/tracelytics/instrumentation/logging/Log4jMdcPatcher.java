package com.tracelytics.instrumentation.logging;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.LogTraceIdScope;

/**
 * Patches `org.apache.log4j.MDC` to include ao.traceId for MDC lookups
 * @author pluk
 *
 */
public class Log4jMdcPatcher extends BaseMdcPatcher {
    private static String CLASS_NAME = Log4jMdcPatcher.class.getName();
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("get", new String[] { "java.lang.String" }, "java.lang.Object", OpType.GET),
                    new MethodMatcher<OpType>("getContext", new String[] { }, "java.util.Hashtable", OpType.GET_CONTEXT));

    private enum OpType {
        GET, GET_CONTEXT;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        if (scope == LogTraceIdScope.DISABLED) {
            return false;
        }
        
        for (Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            if (entry.getValue() == OpType.GET) {
                insertBefore(method, "if (\"" + TRACE_ID_KEY + "\".equals($1)) { return " + CLASS_NAME + ".getLogTraceId(); }", false); 
            } else if (entry.getValue() == OpType.GET_CONTEXT) {
                insertAfter(method, "$_ = " + CLASS_NAME + ".insertLogTraceId($_);", true, false);
            }
        }
    	return true;
    }

   
    public static Hashtable insertLogTraceId(Hashtable result) {
        Metadata metadata = Context.getMetadata();
        if (scope == LogTraceIdScope.ENABLED || (scope == LogTraceIdScope.SAMPLED_ONLY && metadata.isSampled())) {
            if (result == null) {
                result = new Hashtable();
            }
            result.put(TRACE_ID_KEY, metadata.getCompactTraceId());
        }
        return result;
    }
    
}

    