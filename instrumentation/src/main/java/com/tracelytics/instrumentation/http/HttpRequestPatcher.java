package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

import java.util.*;
import java.util.function.BiPredicate;

/**
 * Patches the `java.net.http.HttpRequest` to return a `HttpHeaders` with x-trace ID injected if
 * the request was passed to `java.net.http.HttpClient` to make outbound http request
 */
public class HttpRequestPatcher extends ClassInstrumentation {
    private static final String CLASS_NAME = HttpRequestPatcher.class.getName();
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
            new MethodMatcher<MethodType>("headers", new String[]{ }, "java.net.http.HttpHeaders", MethodType.HEADERS)
    );

    public static final ThreadLocal<Boolean> bypassPatch = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public static BiPredicate<String, String> ACCEPT_ALL = new BiPredicate<String, String>() {
        @Override
        public boolean test(String s, String s2) {
            return true;
        }
    };

    private enum MethodType { HEADERS }

     public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        cc.addMethod(CtNewMethod.make(
                "public java.util.Map tvHeadersAsMap() { " +
                     CLASS_NAME + ".bypassPatch.set(Boolean.TRUE);" + //to avoid triggering the instrumentation recursively
                "    java.util.Map map = headers().map(); " +
                     CLASS_NAME + ".bypassPatch.set(Boolean.FALSE);" +
                "    return map;" +
                "}", cc));
        addTvContextObjectAware(cc);
        tagInterface(cc, HttpRequest.class.getName());

        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertAfter(method,
                    "java.util.Map newMap = " + CLASS_NAME + ".tagContext(this, $_);" +
                    "if (newMap != null) {" +
                    "    return java.net.http.HttpHeaders.of(newMap, " + CLASS_NAME + ".ACCEPT_ALL);" +
                    "} else {" +
                    "    return $_;" +
                    "}", true, false);
        }

        return true;
    }

    /**
     * Returns a new Map with the x-trace ID if injection should be done
     * @param request
     * @param headersObject
     * @return
     */
    public static Map tagContext(HttpRequest request, Object headersObject) {
        if (!bypassPatch.get() && request.getTvContext() != null && request.getTvContext().isValid()) { //then this is the headers of the outbound http request with valid context
            Map<String, List<String>> newMap = new HashMap<String, List<String>>(request.tvHeadersAsMap());
            newMap.put(ClassInstrumentation.XTRACE_HEADER, Collections.singletonList(request.getTvContext().toHexString()));
            return newMap;
        }
        return null;
    }
}
