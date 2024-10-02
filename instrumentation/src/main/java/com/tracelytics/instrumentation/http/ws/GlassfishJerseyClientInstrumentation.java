package com.tracelytics.instrumentation.http.ws;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Instrumentation on Glasshfish Jersey. Take note that this is slightly different from SUN jersey in terms of class hierarchy and execution path therefore an unique
 * instrumentation is necessary.
 * 
 * 
 * @author Patson Luk
 *
 */
public class GlassfishJerseyClientInstrumentation extends BaseWsClientInstrumentation {

    private static final String LAYER_NAME = "rest_client_jersey";
    private static final String CLASS_NAME = GlassfishJerseyClientInstrumentation.class.getName();
    
    // The wrapper code for method `submit`
    private static final String beforeSubmit = Metadata.class.getName() + " xTraceId = " + CLASS_NAME + ".layerEntry($1.getUri(), $1.getMethod(), true);"
            + "if (xTraceId != null) { "
            + "    if ($1.getHeaders() != null) {"
            + "        $1.getHeaders().add(\"" + ServletInstrumentation.XTRACE_HEADER + "\", xTraceId.toHexString());"
            + "    }"
            +      CLASS_NAME + ".storeContext($2, xTraceId);" //Store context for Asynchronous calls, take note that the actual exit event of async calls are traced in GlassfishJerseyResponseCallbackInstrumentation   
            + "}";
    
    // The wrapper code for method `invoke` (before and after)
    private static final String beforeInvoke = CLASS_NAME + ".layerEntry($1.getUri(), $1.getMethod(), false);";
    private static final String afterInvoke = CLASS_NAME + ".layerExit();";
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
            new MethodMatcher<MethodType>("invoke", new String[] { "org.glassfish.jersey.client.ClientRequest"}, "org.glassfish.jersey.client.ClientResponse", MethodType.INVOKE),
            // for Jersey < 2.27
            new MethodMatcher<MethodType>("submit", new String[] {"org.glassfish.jersey.client.ClientRequest", "org.glassfish.jersey.client.ResponseCallback"}, "void", MethodType.SUBMIT_V1),
            // for Jersey >= 2.27, the signature of method submit has changed.
            new MethodMatcher<MethodType>("createRunnableForAsyncProcessing", new String[] {"org.glassfish.jersey.client.ClientRequest", "org.glassfish.jersey.client.ResponseCallback"}, "java.lang.Runnable", MethodType.SUBMIT_V2));
    
    
    private enum MethodType {
        INVOKE, SUBMIT_V1, SUBMIT_V2
    }
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (Map.Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            MethodType type = entry.getValue();
            
            // Unfortunately strings in switch is not supported until JDK 7 
            if (type == MethodType.INVOKE) {
                insertBefore(method, beforeInvoke);
                insertAfter(method, afterInvoke, true);
            } else if (type == MethodType.SUBMIT_V1 || type == MethodType.SUBMIT_V2) {
                insertBefore(method, beforeSubmit, false);
            } else {
                // This should not happen
            }
        }

        return true;
    }

  
    public static Metadata layerEntry(URI resourceURI, String httpMethod, boolean isAsync) {
        if (Context.getMetadata().isSampled()) {
            return layerEntryRest(httpMethod, resourceURI != null ? resourceURI.toString() : null, LAYER_NAME, isAsync);
        } else if (Context.getMetadata().isValid()) {
            return Context.getMetadata();
        } else {
            return null;
        }
    }

    public static void layerExit() {
        layerExitRest(LAYER_NAME);
    }
}