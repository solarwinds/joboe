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
 * Instruments Restlet client. Take note Restlet has several ways of handling http request/response hence X-trace id handling differ from case to case:
 * 
 * Socket (older Restlet version 2.0.x and 2.1.x)
 *  Request - need to inject x-trace ID in <code>RestletHeaderUtilsPatcher</code>
 *  Response - need to restore x-trace ID before layer exit
 * 
 * HttpUrlConnection (newer Restlet version 2.2+)
 *  Request - no need to inject x-trace ID as it's taken care of by <code>HttpUrlConnectionPatcher</code>
 *  Response - need to restore x-trace ID before layer exit
 * 
 * Apache Http client (any Restlet version with httpclient extension)
 *  Request - no need to inject x-trace ID as it's taken care of by ApacheHttpClientInstrumentation
 *  Response - no need to restore x-trace ID as it's taken care of by ApacheHttpClientInstrumentation
 * 
 * 
 * @author pluk
 *
 */
public class RestletClientInstrumentation extends BaseWsClientInstrumentation {
    public static final String CLASS_NAME = RestletClientInstrumentation.class.getName();
    private static final String LAYER_NAME = "rest_client_restlet";

    private enum MethodType { HANDLE };
    
    //take note that we cannot rely on the private/protected methods of restlet as those are changed from version to version. Relying on the public methods of operation seems the most reliable
    //but since those public methods have several variations of signature and calling stack, we have to keep track of the depthThreadLocal to avoid nested instrumentation
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(new MethodMatcher<MethodType>("handle", new String[] { "org.restlet.Request", "org.restlet.Response"}, "void", MethodType.HANDLE, true));
                                                                                  
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtClass targetClass = classPool.get("org.restlet.Client");
        
        if (!cc.equals(targetClass)) { //only instrument the exact class
            return false;
        }
        
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            if (shouldModify(cc, method)) {
                insertBefore(method, CLASS_NAME + ".layerEntryRest($1 != null && $1.getMethod() != null ? $1.getMethod().getName() : null, $1 != null && $1.getResourceRef() != null ? $1.getResourceRef().toString() : null, $1);", false);
                    //extract x-trace id from http response header
                    insertAfter(method, 
                              "String xTraceId = null;"
                            + "if ($2 != null && $2.getAttributes() != null) {"
                            + "    Object headersObject = $2.getAttributes().get(\"org.restlet.http.headers\");"
                            + "    org.restlet.util.Series headers = null;"
                            + "    if (headersObject instanceof org.restlet.util.Series) {"
                            + "        headers = (org.restlet.util.Series)headersObject;"
                            + "        xTraceId = headers.getFirstValue(\"" + ServletInstrumentation.XTRACE_HEADER+ "\");"
                            + "    }"
                            + "}"
                            + CLASS_NAME + ".layerExitRest(getHelper(), xTraceId);", true);
            }
        }
        
        return true;
    }

    public static void layerEntryRest(String httpMethod, String endpointAddress, Object requestObject) {
        if (Context.getMetadata().isSampled()) {
            BaseWsClientInstrumentation.layerEntryRest(httpMethod, endpointAddress, LAYER_NAME);
        }
        
        if (Context.getMetadata().isValid()) {
            if (requestObject instanceof TvContextObjectAware) { //set the context to the request object so we can set the correct x-trace ID even if the request header handling is on a different thread (Restlet 2.1.x)
                ((TvContextObjectAware)requestObject).setTvContext(Context.getMetadata());
            }
        }
    }
    
    public static void layerExitRest(Object helper, String xTraceId) {
        if (isApacheHttpClientHelper(helper)) { //do not continue with the x-trace id as the HttpClientHelper uses Apache client which handles x-trace id already
            BaseWsClientInstrumentation.layerExitRest(LAYER_NAME, (String)null);
        } else {
            BaseWsClientInstrumentation.layerExitRest(LAYER_NAME, xTraceId);
        }
    }
    
    private static boolean isApacheHttpClientHelper(Object helper) {
        return helper != null && helper.getClass().getName().equals("org.restlet.ext.httpclient.HttpClientHelper");
    }
    
    
}



