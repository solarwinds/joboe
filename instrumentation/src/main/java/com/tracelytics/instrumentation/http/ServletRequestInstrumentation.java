package com.tracelytics.instrumentation.http;

import java.util.*;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.Modifier;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.Span;

/**
 *  Modifies the methods of <code>javax.servlet.http.HttpServletRequest</code>
 *  <ol>
 *   <li>Header methods such that extra headers can be added to the ServletRequest even if the headers itself is immutable</li>
 *   <li>Async context method introduced in Servlet 3.0, such that asynchronous mode will get traced properly</li>
 *  </ol>
 * @author pluk
 *
 */
public class ServletRequestInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = ServletRequestInstrumentation.class.getName();
    
    //List of header methods that to be modified to include x-trace id as a request header
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> headerMethodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("getHeader", new String[] { "java.lang.String" }, "java.lang.String", OpType.GET_HEADER, true),
        new MethodMatcher<OpType>("getHeaderNames", new String[] { }, "java.util.Enumeration", OpType.GET_HEADER_NAMES, true),
        new MethodMatcher<OpType>("getHeaders", new String[] { "java.lang.String" }, "java.util.Enumeration", OpType.GET_HEADERS, true));
    

    //List of async context related methods to be modified for asynchronous mode tracing
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> asyncContextMethodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("startAsync", new String[] { }, "javax.servlet.AsyncContext", OpType.START_ASYNC),
         new MethodMatcher<OpType>("startAsync", new String[] { }, "jakarta.servlet.AsyncContext", OpType.START_ASYNC)
    );

    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        // Add method to append extra header to the Servlet Request object
        addExtraHeaderMethods(cc);
        
        // Add our interface so we can access the modified class during layer entry/exit:
        tagInterface(cc, HttpServletRequest.class.getName());
        
        // Modifies the header methods such that if the instance has tvContext, it will also be included in the returned values
        modifyHeaderMethods(cc);
        
        addMethods(cc);
        
        // Modifies the Async context method such to trace asynchronous mode properly
        modifyAsynContextMethods(cc);
        return true;
    }
    

    /**
     * Adds methods to append extra headers to the request object
     */
    private void addExtraHeaderMethods(CtClass cc) throws CannotCompileException {
        //only add these methods if it has not yet been added
        try {
            cc.getField("tvExtraHeaders");
        } catch (NotFoundException e) {
            //not yet patched, then add the methods
            cc.addField(CtField.make("protected java.util.Map tvExtraHeaders = null;", cc));
            cc.addMethod(CtNewMethod.make(
                      "public synchronized void tvSetExtraHeader(String header, String value) {"
                    + "    if (tvExtraHeaders == null) {"
                    + "        tvExtraHeaders = new java.util.HashMap();"
                    + "    }"
                    + "    tvExtraHeaders.put(header, value);"
                    + "}", cc));
            cc.addMethod(CtNewMethod.make(
                    "public synchronized void tvRemoveExtraHeader(String header) {" 
                  + "    if (tvExtraHeaders != null) {"
                  + "        tvExtraHeaders.put(header, null);"
                  + "    }"
                  + "}", cc));
        }        
    }


    /**
     * Modifies the header methods - getHeader, getHeaderNames and getHeaders, such that those methods would include x-trace id as one of the header if the context is set against the ServletRequest instance
     * @param cc
     * @throws CannotCompileException
     */
    private void modifyHeaderMethods(CtClass cc) throws CannotCompileException {
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, headerMethodMatchers).entrySet()) {
            OpType type = methodEntry.getValue();
            CtMethod method = methodEntry.getKey();
            
            if (type == OpType.GET_HEADER) {
                //only return the tagged value if it's non null, otherwise return whatever the actual x-trace value is in the Request header
                insertAfter(method, "if (tvExtraHeaders != null && $1 != null && tvExtraHeaders.containsKey($1)) { Object value = tvExtraHeaders.get($1); return value != null ? value.toString() : null; }", true, false); 
            } else if (type == OpType.GET_HEADER_NAMES) {
                insertAfter(method, "if (tvExtraHeaders != null) { return " + CLASS_NAME + ".appendExtraHeadersToEnumeration($_, tvExtraHeaders); }", true, false);
            } else if (type == OpType.GET_HEADERS) {
                insertAfter(method, "if (tvExtraHeaders != null && $1 != null && tvExtraHeaders.containsKey($1)) { return " + CLASS_NAME + ".getExtraHeaderEnumeration(tvExtraHeaders, $1); }", true, false);
            }
        }
    }
    
    /**
     * Modifies the AsyncContext methods to handle Asynchronous mode 
     * @param cc
     * @throws NotFoundException
     * @throws CannotCompileException
     */
    private void modifyAsynContextMethods(CtClass cc) throws NotFoundException, CannotCompileException {
        for (Entry<CtMethod, OpType> entry : findMatchingMethods(cc, asyncContextMethodMatchers).entrySet()) {
            if (entry.getValue() == OpType.START_ASYNC) {
                CtMethod startAsyncMethod = entry.getKey();
                if (shouldModify(cc, startAsyncMethod)) {
                    insertAfter(startAsyncMethod, CLASS_NAME + ".traceStartAsync($_, this);", true, false);
                }
            }
        }
        
    }
    
    private void addMethods(CtClass cc) throws CannotCompileException {
        // Add AsyncContext getter/setter if it is supported

        /*
         * indicate whether there are concrete getAsyncContext and getDispatcherType to invoke,
         * take note that sometimes a framework that does not support servlet 3.0 might be deployed in an app server with servlet 3.0.
         * In such case, getMethod might return 3.0 interface abstract method, but during method invocation, AbstractMethodError will be thrown
         */
        boolean hasConcreteJavaxMethodsToInvoke = false;
        boolean hasConcreteJakartaMethodsToInvoke = false;

        try {
            hasConcreteJavaxMethodsToInvoke = !Modifier.isAbstract(cc.getMethod("getDispatcherType", "()Ljavax/servlet/DispatcherType;").getModifiers());
        } catch (NotFoundException e1) {
            try {
                hasConcreteJakartaMethodsToInvoke = !Modifier.isAbstract(cc.getMethod("getDispatcherType", "()Ljakarta/servlet/DispatcherType;").getModifiers());
            } catch (NotFoundException e2) {
                logger.debug("Cannot find getDispatcherType, probably running on servlet spec older than 3.0");
            }
        }

        if (hasConcreteJavaxMethodsToInvoke) {
            cc.addMethod(CtNewMethod.make("public boolean tvIsAsyncDispatch() { return getDispatcherType() == javax.servlet.DispatcherType.ASYNC; }", cc));
        } else if (hasConcreteJakartaMethodsToInvoke) {
            cc.addMethod(CtNewMethod.make("public boolean tvIsAsyncDispatch() { return getDispatcherType() == jakarta.servlet.DispatcherType.ASYNC; }", cc));
        } else {
	        cc.addMethod(CtNewMethod.make("public boolean tvIsAsyncDispatch() { return false; }", cc));
        }
    }

    /**
     * Adds extra keys to the enumeration returned by getHeaderNames method
     * @param originalEnumeration
     * @return
     */
    public static Enumeration<String> appendExtraHeadersToEnumeration(Enumeration<String> originalEnumeration, Map<String, String> extraHeaders) {
        List<String> headers;
        if (originalEnumeration == null) {
            headers = new ArrayList<String>();
        } else {
            headers = Collections.list(originalEnumeration);
        }
        
        for (Entry<String, String> extraHeaderKeyValue : extraHeaders.entrySet()) {
            if (extraHeaderKeyValue.getValue() != null && //for header that is removed from map, we set the value to null, need to exclude those
                !headers.contains(extraHeaderKeyValue.getKey())) {
                headers.add(extraHeaderKeyValue.getKey());
            }
        }
        
        return Collections.enumeration(headers);
    }
    
    /**
     * Returns a enumeration of 1 value if getting from extra header
     * @param extraHeaders
     * @param key
     * @return
     */
    public static Enumeration<String> getExtraHeaderEnumeration(Map<String, String> extraHeaders, String key) {
        return Collections.enumeration(Collections.singleton(extraHeaders.get(key)));
    }
    
    /**
     * When startAsync is called, we want to flag the AsyncContext as active, set the context object
     * 
     * @param asyncContextObject
     * @param requestObject
     */
    public static void traceStartAsync(Object asyncContextObject, Object requestObject) {
        if (asyncContextObject instanceof ServletAsyncContext && ((ServletAsyncContext)asyncContextObject).tvGetSpanStack() == null) { //only capture if it has not been captured yet
            List<Span> currentSpanStack = ServletInstrumentation.getCurrentServletSpanStack();
            ((ServletAsyncContext)asyncContextObject).tvSetSpanStack(new ArrayList<Span>(currentSpanStack)); //make a clone, otherwise when the servlet method exit would clear the original list(but request is now async, so not completed yet)
            ServletInstrumentation.setAsyncStarted(true);

            if (requestObject instanceof HttpServletRequest) { //need to set this to the request object by setAttribute in order to persist through wrappings, see https://github.com/librato/joboe/issues/548
                ((HttpServletRequest) requestObject).setAttribute(ServletInstrumentation.ASYNC_CONTEXT_ATTRIBUTE_NAME, asyncContextObject); 
            }
        }
    }
    
    
    private enum OpType {
        GET_HEADER, GET_HEADER_NAMES, GET_HEADERS, START_ASYNC
    }
}


