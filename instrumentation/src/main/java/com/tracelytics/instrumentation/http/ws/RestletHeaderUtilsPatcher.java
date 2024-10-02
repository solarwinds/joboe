package com.tracelytics.instrumentation.http.ws;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;

/**
 * Adds X-Trace to the Http request sent by the older version of Restlet client which uses Socket. For newer version, it uses HttpUrlConnection which we have already inserted the X-Trace into 
 * the http request headers automatically
 * @author pluk
 *
 */
public class RestletHeaderUtilsPatcher extends BaseWsClientInstrumentation {
    
    public static final String CLASS_NAME = RestletHeaderUtilsPatcher.class.getName();
    
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(new MethodMatcher<Object>("addRequestHeaders", new String[] { "org.restlet.Request", "org.restlet.util.Series"}, "void"));
        
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
    
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            if (shouldModify(cc, method)) {
                insertBefore(method, 
                        "if ($1 instanceof " + TvContextObjectAware.class.getName() + " && "
                      + "    $2 != null && $2.getFirstValue(\"" + ServletInstrumentation.XTRACE_HEADER + "\") == null) { "
                      + "    String xTrace = " + CLASS_NAME + ".getXTraceId($1);"
                      + "    if (xTrace != null) {"
                      + "        $2.set(\"" + ServletInstrumentation.XTRACE_HEADER + "\", xTrace); "
                      + "    }"
                      + "}", false);
            }
        }
        return true;
    }
    
    public static String getXTraceId(Object requestObject) {
        TvContextObjectAware request = (TvContextObjectAware) requestObject;
        if (request.getTvContext() != null && request.getTvContext().isValid()) { 
            return request.getTvContext().toHexString(); 
        } else {
            return null;
        }
    }
}



