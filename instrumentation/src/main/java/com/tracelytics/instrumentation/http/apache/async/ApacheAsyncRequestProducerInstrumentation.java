package com.tracelytics.instrumentation.http.apache.async;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ContextPropagationPatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.util.HttpUtils;

/**
 * Instruments `org.apache.http.nio.protocol.HttpAsyncRequestProducer` to capture useful info such as http method and url 
 * from the `HttpRequest` object created by `generateRequest` call
 * 
 * It also sets x-trace ID to the outbound request header 
 *   
 * @author pluk
 *
 */
public class ApacheAsyncRequestProducerInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = ApacheAsyncRequestProducerInstrumentation.class.getName();
    private static String LAYER_NAME = "apache-async-http-client";
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
        new MethodMatcher<MethodType>("generateRequest", new String[0], "org.apache.http.HttpRequest", MethodType.GENERATE_REQUEST)
    );
    
    //Flag for whether hide query parameters as a part of the URL or not. By default false 
    private static boolean hideQuery = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.APACHE_HTTP) : false;
    
    private enum MethodType {
        GENERATE_REQUEST
    }
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        addTvContextObjectAware(cc);
         
        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            insertAfter(method,
                      "String xTraceId = " + CLASS_NAME + ".afterGenerateRequest(this, "
                    + "    ($_ != null && $_.getRequestLine() != null) ? $_.getRequestLine().getUri() : null,"
                    + "    ($_ != null && $_.getRequestLine() != null) ? $_.getRequestLine().getMethod() : null,"
                    + "    getTarget() != null ? getTarget().toURI() : null);"
                    + "if ($_ != null && xTraceId != null) {"
                    + "    ((org.apache.http.HttpRequest)$_).setHeader(\"" + ServletInstrumentation.XTRACE_HEADER + "\", xTraceId);"
                    + "}", true, false);
        }
        return true;
    }
    
    public static String afterGenerateRequest(Object producerObject, String uriString, String method, String hostUri) {
        if (ContextPropagationPatcher.restoreContext(producerObject)) {
            Metadata metadata = Context.getMetadata();
            String xTraceID = Context.getMetadata().toHexString(); //take the entry event x-trace id
            
            if (metadata.isSampled()) {
                Event event = Context.createEvent();
    
                event.addInfo("Layer", LAYER_NAME,
                              "Label", "info");
                              
                if (method != null) {
                    event.addInfo("HTTPMethod", method);
                }
                 
                if (uriString != null) {
                    try {
                        URI uri = new URI(uriString);
                        String path = uri.getPath();
    
                        if (path == null) {
                            path = "/";
                        }
    
                        String query = uri.getQuery();
                        if (query != null && !hideQuery) {
                            path += "?" + query;
                        }
    
                        String remoteUrl = hideQuery ? HttpUtils.trimQueryParameters(uri.toString()) : uri.toString();
                        
                        if (!uri.isAbsolute() && hostUri != null) { //if it's a relative path, prepend the host part 
                            remoteUrl = hostUri + remoteUrl;
                        }
                        
                        event.addInfo("RemoteURL", remoteUrl);
                    } catch(URISyntaxException ex) {
                        // Bad URI? Nothing we can really do.
                        logger.debug("Invalid URI: " + uriString, ex);
                    }
                }
                event.report();
            }
            
            
            ContextPropagationPatcher.resetContext(producerObject);
            return xTraceID;
        } else {
            return null;
        }
        
    }
}