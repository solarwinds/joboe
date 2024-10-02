package com.tracelytics.instrumentation.http.apache;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
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
 * Instruments Apache Http Client 3.x and 4.x
 */
public class ApacheHttpClientInstrumentation extends ClassInstrumentation {
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
        new MethodMatcher<MethodType>("executeMethod", new String[]{ "org.apache.commons.httpclient.HostConfiguration", "org.apache.commons.httpclient.HttpMethod", "org.apache.commons.httpclient.HttpState"}, "int", MethodType.EXECUTE_V3, true), //3.x
        new MethodMatcher<MethodType>("execute", new String[]{ "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.protocol.HttpContext"}, "org.apache.http.HttpResponse", MethodType.EXECUTE_V4, true), //4.0 - 4.2
        new MethodMatcher<MethodType>("doExecute", new String[]{ "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.protocol.HttpContext"}, "org.apache.http.client.methods.CloseableHttpResponse", MethodType.DO_EXECUTE_V4_3, true) //4.3+
    );
    
    //Flag for whether hide query parameters as a part of the URL or not. By default false 
    private static boolean hideQuery = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.APACHE_HTTP) : false;
    
    private enum MethodType {
        EXECUTE_V3, EXECUTE_V4, DO_EXECUTE_V4_3
    }
    
    private static ThreadLocal<Integer> depthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
         
        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod execMethod = entry.getKey();
            MethodType type = entry.getValue();
            if (shouldModify(cc, execMethod)) {
                try {
                    if (type == MethodType.EXECUTE_V3) { // Instrument executeMethod (3.x API)
                        modifyHttpClient3(execMethod);
                    } else if (type == MethodType.EXECUTE_V4 || type == MethodType.DO_EXECUTE_V4_3) {
                        modifyHttpClient4(execMethod, cc);
                    }
                } catch(CannotCompileException ex) {
                    logger.warn("Unable to instrument executeMethod in " + cc.getName(), ex);
                } 
            }
        }
        
        return true;
    }
    
    private void modifyHttpClient4(CtMethod execMethod, CtClass clientClass) throws CannotCompileException, NotFoundException {
        boolean isSupportGetParams = isSupportGetParams(clientClass);
        
        StringBuffer injectingCode = new StringBuffer(
                "String tlysHost = null;" +
                "String tlysProtocol = null;" +
                "int tvPort = -1;" +
                "if ($1 != null) {" +
                "    tlysHost = $1.getHostName();" +
                "    tvPort = $1.getPort();" +
                "    tlysProtocol = $1.getSchemeName();" +
                "}");
        
        if (isSupportGetParams) { //then try to call getParams to retrieve it from the client, only useful for the older deprecated AbstractHttpClient
            injectingCode.append(
                " else if ($2 instanceof org.apache.http.client.methods.HttpUriRequest && " +  //then we need to retrieve the host embedded in the params of the current client
                "           ((org.apache.http.client.methods.HttpUriRequest)$2).getURI() != null && " +
                "           !((org.apache.http.client.methods.HttpUriRequest)$2).getURI().isAbsolute()) {" +
                "    Object hostObject = getParams().getParameter(org.apache.http.client.params.ClientPNames.DEFAULT_HOST);" +
                "    if (hostObject instanceof org.apache.http.HttpHost) {" +
                "        tlysHost = ((org.apache.http.HttpHost)hostObject).getHostName();" +
                "        tvPort = ((org.apache.http.HttpHost)hostObject).getPort();" +
                "        tlysProtocol = ((org.apache.http.HttpHost)hostObject).getSchemeName();" +
                "    }" +
                "}");
        }
        
        injectingCode.append(CLASS_NAME +".beforeExecute_v4($2, $2.getRequestLine().getUri(), tlysHost, tvPort, tlysProtocol, $2.getRequestLine().getMethod());");
        insertBefore(execMethod, injectingCode.toString(), false);
        addErrorReporting(execMethod, IOException.class.getName(), CLASS_NAME, classPool);
        insertAfter(execMethod, "{ if ( $_ != null ) { " +
               "   org.apache.http.Header tlysHdr = $_.getFirstHeader(\"" + ServletInstrumentation.XTRACE_HEADER + "\");" +
               "   String tlysXTrace = null;" +
               "   if (tlysHdr != null) { tlysXTrace = tlysHdr.getValue(); }" +
                   CLASS_NAME + ".afterExecute(tlysXTrace, ($_.getStatusLine() != null ? $_.getStatusLine().getStatusCode() : 0));" +
               " } else { " +
                   CLASS_NAME + ".afterExecute(null, 0);" +
               " } }", true);
    }

    /**
     * Checks whether the getParams method is supported for this class. Since 4.x, the new InternalHttpClient throws UnsupportedOperationException on getParams() method
     *  
     * @param clientClass
     * @return
     */
    private boolean isSupportGetParams(CtClass clientClass) {
        try {
            CtClass abstractHttpClientClass = classPool.get("org.apache.http.impl.client.AbstractHttpClient");
            return clientClass.subtypeOf(abstractHttpClientClass); //if the class is subtype of AbstractHttpClient, which supports getParams()
        } catch (NotFoundException e) {
            logger.debug("Failed to load class org.apache.http.impl.client.AbstractHttpClient, probably running a newer version of apache with this deprecated class removed");
        } 
        return false;
    }

    private void modifyHttpClient3(CtMethod execMethod)
        throws CannotCompileException, NotFoundException {
        insertBefore(execMethod, 
                "String tlysHost = null;" +
                "String tlysProtocol = null;" +
                "int tvPort = -1;" +
                "if ($1 != null) {" +
                "    tlysHost = $1.getHost();" +
                "    tvPort = $1.getPort();" +
                "    tlysProtocol = $1.getProtocol() != null ? $1.getProtocol().getScheme() : null;" +
                "} else if (!$2.getURI().isAbsoluteURI()) {" + //then we need to retrieve the host associated with the current client
                "    org.apache.commons.httpclient.HostConfiguration hostConfig = getHostConfiguration();" +
                "    if (hostConfig != null) {" +
                "        tlysHost = hostConfig.getHost();" +
                "        tvPort = hostConfig.getPort();" +
                "        tlysProtocol = hostConfig.getProtocol() != null ? hostConfig.getProtocol().getScheme() : null;" +
                "    }" +
                "}" + 
                CLASS_NAME +".beforeExecute_v3($2, $2.getURI().toString(), tlysHost, tvPort, tlysProtocol, $2.getName());", false);
        
        addErrorReporting(execMethod, IOException.class.getName(), CLASS_NAME, classPool);
        insertAfter(execMethod, "{ org.apache.commons.httpclient.Header tlysHdr = $2.getResponseHeader(\"" +ServletInstrumentation.XTRACE_HEADER  + "\");" +
               " String tlysXTrace = null;" +
               " if (tlysHdr != null) { tlysXTrace = tlysHdr.getValue(); } " +
                 CLASS_NAME + ".afterExecute(tlysXTrace, $_);" +
               "}", true);
    }


    // v3 and v4 methods are almost identical, except for how we set the request headers... ugh.
    public static void beforeExecute_v3(Object httpMethodObj, String uri, String host, int port, String protocol, String method) {
        ApacheHttpMethod http = (ApacheHttpMethod) httpMethodObj;
        
        Metadata metadata = Context.getMetadata(); 
        if (metadata.isSampled()) {
            if (shouldStartExtent()) {
                httpClientEntry(uri, host, port, protocol, method);
            }
        }
        
        // Add X-Trace header to HTTP request:
        if (metadata.isValid()) {
            http.setRequestHeader(ServletInstrumentation.XTRACE_HEADER, metadata.toHexString());
        }
    }

    public static void beforeExecute_v4(Object httpReqMsgObj, String uri, String host, int port, String protocol, String method) {
        ApacheHttpMessage http = (ApacheHttpMessage) httpReqMsgObj;
        
        Metadata metadata = Context.getMetadata();
        if (metadata.isSampled()) {
            if (shouldStartExtent()) {
                httpClientEntry(uri, host, port, protocol, method);
            }
        }
        
        // Add X-Trace header to HTTP request:
        if (metadata.isValid()) {
            http.setHeader(ServletInstrumentation.XTRACE_HEADER, metadata.toHexString());
        }

    }

    private static void httpClientEntry(String uriString, String host, int port, String protocol, String method) {
        Event event = Context.createEvent();

        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry",
                      "Spec", "rsc",
                      "IsService", Boolean.TRUE,
                      "HTTPMethod", method);

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

            String remoteUrl;
            if (host != null && protocol != null) {
                remoteUrl = protocol + "://" + (port != -1 ? (host + ":" + port) : host) + path;
            } else { //use the URI directly
                remoteUrl = hideQuery ? HttpUtils.trimQueryParameters(uri.toString()) : uri.toString();
            } 
            event.addInfo("RemoteURL", remoteUrl);
        } catch(URISyntaxException ex) {
            // Bad URI? Nothing we can really do.
            logger.debug("Invalid URI: " + uriString, ex);
        }

        ClassInstrumentation.addBackTrace(event, 2, Module.APACHE_HTTP);
        event.report();
    }

    public static void afterExecute(String xTrace, int statusCode) {
        if (shouldEndExtent()) {
            Event event = Context.createEvent();
    
            // HTTP X-Trace header is only present if remote server is instrumented
            if (xTrace != null) {
                event.addEdge(xTrace);
            }
    
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit",
                          "HTTPStatus", statusCode);
    
            event.report();
        }
    }
    
    /**
     * Checks whether the current instrumentation should start a new extent. If there is already an active extent, then do not start one
     * @return
     */
    protected static boolean shouldStartExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth + 1);

        if (currentDepth == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether the current instrumentation should end the current extent. If this is the active extent being traced, then ends it
     * @return
     */
    protected static boolean shouldEndExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth - 1);

        if (currentDepth == 1) {
            return true;
        } else {
            return false;
        }
    }

    private static String CLASS_NAME = ApacheHttpClientInstrumentation.class.getName();
    private static String LAYER_NAME = "apache_http_client";
}