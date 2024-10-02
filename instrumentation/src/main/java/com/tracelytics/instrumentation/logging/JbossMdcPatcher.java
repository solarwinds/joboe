package com.tracelytics.instrumentation.logging;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.LogTraceIdScope;

import java.util.*;
import java.util.Map.Entry;

/**
 * Patches `org.jboss.logmanager.MDC` to include ao.traceId for MDC lookups
 * @author pluk
 *
 */
public class JbossMdcPatcher extends BaseMdcPatcher {
    private static String CLASS_NAME = JbossMdcPatcher.class.getName();
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("get", new String[] { "java.lang.String" }, "java.lang.String", OpType.GET), //jboss MDC 1.x
                    new MethodMatcher<OpType>("getObject", new String[] { "java.lang.String" }, "java.lang.Object", OpType.GET)); //jboss MDC 2.x

    private enum OpType {
        GET;
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
            }
        }
    	return true;
    }
}

    