package com.tracelytics.instrumentation.logging;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.config.LogTraceIdScope;

/**
 * Patches `org.apache.logging.log4j.ThreadContext` to include ao.traceId for ThreadContext(MDC) lookups
 * 
 * 
 * Take note that we cannot modify the getContext, getThreadContextMap and getImmutableContext method's return value as it's quite challenging to return object with
 * the right type especially for getImmutableContext and getThreadContextMap.
 * 
 * Therefore we patch those methods to call `put`/`remove` for "ao.traceId" right before the method body. 
 * 
 * For cleanup, we cannot call `remove` on method body exit as the getter method returns a "view" of the underlying map storage, 
 * therefore clearing it on method body exit will affect the returned value as well. Instead we will simply remove the key at the
 * beginning of the getter method if `ao.traceId` should no longer be returned 
 * 
 * Other "read" methods such as `get`, `containsKey` and `isEmpty` have to be patched as well just to ensure all read operations take into consideration of `ao.traceId`
 * 
 * @author pluk
 *
 */
public class Log4j2ThreadContextPatcher extends BaseMdcPatcher {
    private static String CLASS_NAME = Log4j2ThreadContextPatcher.class.getName();
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("get", new String[] { "java.lang.String" }, "java.lang.String", OpType.GET),
                    new MethodMatcher<OpType>("containsKey", new String[] { "java.lang.String" }, "boolean", OpType.CONTAINS_KEY),
                    new MethodMatcher<OpType>("isEmpty", new String[] { }, "boolean", OpType.IS_EMPTY),
                    new MethodMatcher<OpType>("getImmutableContext", new String[] { }, "java.util.Map", OpType.GET_CONTEXT),
                    new MethodMatcher<OpType>("getThreadContextMap", new String[] { }, "org.apache.logging.log4j.spi.ReadOnlyThreadContextMap", OpType.GET_CONTEXT),
                    new MethodMatcher<OpType>("getContext", new String[] { }, "java.util.Map", OpType.GET_CONTEXT));

    private enum OpType {
        GET, CONTAINS_KEY, IS_EMPTY, GET_CONTEXT;
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
            } else if (entry.getValue() == OpType.CONTAINS_KEY) {
                insertBefore(method, "if (\"" + TRACE_ID_KEY + "\".equals($1)) { return " + CLASS_NAME + ".getLogTraceId() != null; }", false);
            } else if (entry.getValue() == OpType.IS_EMPTY) {
                insertBefore(method, "if (" +  CLASS_NAME + ".getLogTraceId() != null) { return false; }", false);
            } else if (entry.getValue() == OpType.GET_CONTEXT) {
                insertBefore(method, 
                                  "String logTraceId = " + CLASS_NAME + ".getLogTraceId(); "
                                + "if (logTraceId != null) {"
                                + "    put(\"" + TRACE_ID_KEY + "\", logTraceId);"
                                + "} else {"
                                + "    remove(\"" + TRACE_ID_KEY + "\");"
                                + "}"
                                , false);
                         
            }
        }
    	return true;
    }
}

    