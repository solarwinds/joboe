package com.tracelytics.instrumentation.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.util.HttpUtils;

/**
 * Instruments HttpURLConnection for its interaction with remote server.
 * 
 * Due to the input stream/output stream design of HttpURLConnection, it is hard to define boundaries of a complete http call (as the caller might or might not read/close the streams).
 * 
 * However, it is still possible to capture meaningful information when the interactions are broken down into 2 categories
 * <ol>
 *  <li>http-connect - This refers to the initial DNS lookup and handshake (for https) involved, we can simply capture the HttpURLConnection.connect method for this</li>
 *  <li>http-<method> - for example http-get, http-post. This refers to the time the client is blocked while waiting for the first reply from the remote server, this does
 *  NOT include all the input stream operations entail which varies based on the access pattern of the caller. Take note that writing of the data to the remote host (from the outputstream to the remote host)
 *  usually is triggered by getInputStream call. Therefore if we capture the blocking time of getInputStream, it will be able to capture from the start of writing to remote host to the first response received.
 *  However in some rare cases, output MIGHT be sent to the remote server before getInputStream is invoked, and that usually happens for more complicated usage of the HttpURLConnection with wrapped
 *  output stream (details documented in https://github.com/tracelytics/joboe/issues/452)</li>
 * </ol>
 * 
 * Besides reporting the above extents, we also modify the request header to include our x-trace id (if not already present). In some edge cases for oracle jdk 8 HttpURLConnection 
 * (details in https://github.com/tracelytics/joboe/issues/458), headers cannot be set in this level. Therefore, we also apply patching to <code>sun.net.www.http.HttpClient</code> by
 * <code>com.tracelytics.instrumentation.http.SunHttpClientPatcher</code> to handle those edge cases
 * 
 * @author pluk
 *
 */
public class HttpURLConnectionInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = HttpURLConnectionInstrumentation.class.getName();
    private static final String LAYER_NAME = "http";
    
    //Flag for whether hide query parameters as a part of the URL or not. By default false 
    private static boolean hideQuery = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.URL_CONNECTION) : false;
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        if (cc.subtypeOf(classPool.get("javax.net.ssl.HttpsURLConnection"))) { //do not instrument the HttpsURLConnection directly, rely on the HttpURLConnection instance that it delegates to
            return false;
        }
        
        cc.addField(CtField.make("private boolean tvInputStreamReported;", cc));
        cc.addMethod(CtNewMethod.make("public boolean isTvInputStreamReported() { return tvInputStreamReported; }", cc));
        cc.addMethod(CtNewMethod.make("public void setTvInputStreamReported(boolean inputStreamReported) { tvInputStreamReported = inputStreamReported; }", cc));
        
        cc.addField(CtField.make("private String tvEntryHttpMethod;", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetEntryHttpMethod() { return tvEntryHttpMethod; }", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetEntryHttpMethod(String httpMethod) { tvEntryHttpMethod = httpMethod; }", cc));
        
        cc.addField(CtField.make("private java.net.URL tvEntryUrl;", cc));
        cc.addMethod(CtNewMethod.make("public java.net.URL tvGetEntryUrl() { return tvEntryUrl; }", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetEntryUrl(java.net.URL entryUrl) { tvEntryUrl = entryUrl; }", cc));
        
        cc.addField(CtField.make("private String tvEntryXTraceId;", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetEntryXTraceId() { return tvEntryXTraceId; }", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetEntryXTraceId(String entryXTraceId) { tvEntryXTraceId = entryXTraceId; }", cc));
        
        
        tagInterface(cc, com.tracelytics.instrumentation.http.HttpURLConnection.class.getName());
        
        
        CtMethod connectMethod = cc.getMethod("connect", "()V");
        if (shouldModify(cc, connectMethod)) {
            insertBefore(connectMethod, CLASS_NAME +".connectEntry(this, connected);", false);
            addErrorReporting(connectMethod, IOException.class.getName(), LAYER_NAME + "-connect", classPool);
            insertAfter(connectMethod, CLASS_NAME +".connectExit();", true);
        }
        
        //also patch to add x-trace header for getInputStream and getOutputStream as Jdk 8 calls connect() within getInputStream()/getOutputStream(). Such an ordering
        //triggers "java.lang.IllegalStateException: Already connected" if we add request property at connect(). For details refer to 
        //https://github.com/tracelytics/joboe/pull/244
        CtMethod getInputStreamMethod = cc.getMethod("getInputStream", "()Ljava/io/InputStream;");
        if (shouldModify(cc, getInputStreamMethod)) {
            insertBefore(getInputStreamMethod, CLASS_NAME +".getInputStreamEntry(this, connected);", false);
            addErrorReporting(getInputStreamMethod, IOException.class.getName(), null, classPool);
            insertAfter(getInputStreamMethod, CLASS_NAME +".getInputStreamExit(this.responseCode, this);", true);
        }
        
        CtMethod getOutputStreamMethod = cc.getMethod("getOutputStream", "()Ljava/io/OutputStream;");
        if (shouldModify(cc, getOutputStreamMethod)) {
            insertBefore(getOutputStreamMethod, CLASS_NAME +".getOutputStreamEntry(this, connected);", false);
        }
        
        return true;
    }
    
    /**
     * Add x-trace ID to the request header if it's not yet set. 
     * 
     * Take note that it might pre-generate an x-trace ID if sampled and use that as the header. 
     * 
     * The pre-generated ID will then be set against the current connection and use as the task ID for http-method span entry event. 
     * 
     * This is necessary as setting request header upon http-method span entry is probably too late as the connection is already established and no header modification is allowed
     * 
     * 
     * @param connection
     * @param connected
     */
    public static void addXTraceHeader(HttpURLConnection connection, boolean connected) {
      //do not attempt to modify header if it's already connected, otherwise it would throws IllegalStateException according to the api here 
      //https://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html#getRequestProperty(java.lang.String)
      //though it is actually not strictly enforced by the actual implementation (for example the oracle/sun one does not throw exception even if it's connected)
        if (connected) { 
            return;
        }
        
        Metadata md = Context.getMetadata();
        if (md.isValid()) {
            try {
                if (connection.getRequestProperty(ServletInstrumentation.XTRACE_HEADER) == null) { //only attempt to add header if it's not set by other layers
                    //pre-generate x-trace id and set to header only if it's not a OUTPUT request 
                    //OUTPUT request might only do getOutputStream and not getInputStream therefore pre-generating x-trace ID for OUTPUT request 
                    //might result as including a generated x-trace id that's never used
                    if (md.isSampled() && !connection.getDoOutput()) { 
                        if (((com.tracelytics.instrumentation.http.HttpURLConnection)connection).tvGetEntryXTraceId() == null) { //only generate if it has not been generated yet
                            //then generate the http-method entry x-trace id, we have to do this ahead of time as adding x-trace header on http-method entry is too late 
                            //(connected already, no header modification allowed)
                            Metadata entryMetadata = new Metadata(md);
                            entryMetadata.randomizeOpID();
                            String entryXTraceId = entryMetadata.toHexString();
                            connection.addRequestProperty(ServletInstrumentation.XTRACE_HEADER, entryXTraceId);
                              
                            ((com.tracelytics.instrumentation.http.HttpURLConnection)connection).tvSetEntryXTraceId(entryXTraceId);
                        }
                    } else { //otherwise use the existing md
                        connection.addRequestProperty(ServletInstrumentation.XTRACE_HEADER, md.toHexString());
                    }
                }
            } catch (IllegalStateException e) {
                //for java 8 redirects, it could clear up the request header while not allowing setting it again, so this is expected for jdk 8 https://github.com/tracelytics/joboe/issues/458
                logger.debug("Failed attempt to get/set request property on the connnection. Probably connecting on oracle jdk 8...");
            }
        }
    }
    
    public static void getInputStreamEntry(HttpURLConnection connection, boolean connected) {
        if (Context.getMetadata().isValid()) {
            addXTraceHeader(connection, connected);
        }
        
        if (Context.getMetadata().isSampled() && shouldStartInputStreamExtent() && !((com.tracelytics.instrumentation.http.HttpURLConnection)connection).isTvInputStreamReported()) {
            String entryXTraceId = ((com.tracelytics.instrumentation.http.HttpURLConnection)connection).tvGetEntryXTraceId();
            Event entryEvent;
            
            if (entryXTraceId != null) { //if there's a pre-generated entry id, use it (with only getInputStream operations, it's safe to use it)
                try {
                    entryEvent = Context.createEventWithID(entryXTraceId);
                } catch (OboeException e) {
                    logger.warn(e.getMessage(), e);
                    entryEvent = Context.createEvent();
                }
            } else { 
                entryEvent = Context.createEvent();
            }
            String httpMethod = connection.getRequestMethod();
            entryEvent.addInfo("Layer", getHttpActionLayerName(httpMethod),
                               "Label", "entry",
                               "IsService", Boolean.TRUE,
                               "Spec", "rsc",
                               "JavaMethod", "getInputStream");
            
            if (httpMethod != null) {
                entryEvent.addInfo("HTTPMethod", httpMethod);
                ((com.tracelytics.instrumentation.http.HttpURLConnection)connection).tvSetEntryHttpMethod(httpMethod); //set it so the exit can have consistent layer name through redirects
            }
            
            URL url = connection.getURL();
            if (url != null) {
                entryEvent.addInfo("RemoteURL", hideQuery ? HttpUtils.trimQueryParameters(url.toString()) : url.toString());
                ((com.tracelytics.instrumentation.http.HttpURLConnection)connection).tvSetEntryUrl(url);
            }
            
            addBackTrace(entryEvent, 1, Module.URL_CONNECTION);
            entryEvent.report();
        }
    }
    
    public static void getInputStreamExit(int statusCode, HttpURLConnection connection) {
        if (shouldEndInputStreamExtent() && !((com.tracelytics.instrumentation.http.HttpURLConnection)connection).isTvInputStreamReported()) {
            Event exitEvent = Context.createEvent(); 
            exitEvent.addInfo("HTTPStatus", statusCode,
                               "Label", "exit",
                               "Layer", getHttpActionLayerName(((com.tracelytics.instrumentation.http.HttpURLConnection)connection).tvGetEntryHttpMethod())); //use same name as the entry 
                               
            //check if it's redirected
            URL url = connection.getURL();
            if (url != null && !url.equals(((com.tracelytics.instrumentation.http.HttpURLConnection)connection).tvGetEntryUrl())) {
                exitEvent.addInfo("RedirectRemoteURL", hideQuery ? HttpUtils.trimQueryParameters(url.toString()) : url.toString());
                exitEvent.addInfo("RedirectHTTPMethod", connection.getRequestMethod());
            } else if (connection.getRequestMethod() != null && !connection.getRequestMethod().equals(((com.tracelytics.instrumentation.http.HttpURLConnection)connection).tvGetEntryHttpMethod())) { 
              //even if url is same, it could have redirected to same URL but with different method
                exitEvent.addInfo("RedirectHTTPMethod", connection.getRequestMethod());
            }
            
            exitEvent.report();
            
            //clean up
            ((com.tracelytics.instrumentation.http.HttpURLConnection)connection).setTvInputStreamReported(true);
            ((com.tracelytics.instrumentation.http.HttpURLConnection)connection).tvSetEntryUrl(null);
            ((com.tracelytics.instrumentation.http.HttpURLConnection)connection).tvSetEntryHttpMethod(null);
            ((com.tracelytics.instrumentation.http.HttpURLConnection)connection).tvSetEntryXTraceId(null);
        }
    }
    
    public static void getOutputStreamEntry(HttpURLConnection connection, boolean connected) {
        if (Context.getMetadata().isValid()) {
            addXTraceHeader(connection, connected);
        }
    }
    
    private static String getHttpActionLayerName(String httpMethod) {
        return httpMethod != null ? (LAYER_NAME + "-" + httpMethod.toLowerCase()) : LAYER_NAME;
    }
    
    public static void connectEntry(HttpURLConnection connection, boolean connected) {
        if (Context.getMetadata().isValid()) {
            addXTraceHeader(connection, connected);
        }
        
        if (Context.getMetadata().isSampled() && shouldStartConnectExtent()) {
            Event entryEvent = Context.createEvent(); 
            entryEvent.addInfo("Layer", LAYER_NAME + "-connect",
                               "Label", "entry",
                               "IsService", Boolean.TRUE,
                               "Spec", "rsc",
                               "JavaMethod", "connect");
                               
            URL url = connection.getURL();
            if (url != null) {
                entryEvent.addInfo("RemoteURL", hideQuery ? HttpUtils.trimQueryParameters(url.toString()) : url.toString());
            }
             
            addBackTrace(entryEvent, 1, Module.URL_CONNECTION);
            entryEvent.report();
        }
        
    }
    
    public static void connectExit() {
        if (shouldEndConnectExtent()) {
            Event exitEvent = Context.createEvent(); 
            exitEvent.addInfo("Layer", LAYER_NAME + "-connect",
                               "Label", "exit");
                               
            
            exitEvent.report();
        }
    }
    
    
    private static ThreadLocal<Integer> getInputStreamDepthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };
    
    private static ThreadLocal<Integer> connectDepthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };


    /**
     * Checks whether the current instrumentation should start a new extent. If there is already an active extent, then do not start one
     * @return
     */
    protected static boolean shouldStartConnectExtent() {
        int currentDepth = connectDepthThreadLocal.get();
        connectDepthThreadLocal.set(currentDepth + 1);

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
    protected static boolean shouldEndConnectExtent() {
        int currentDepth = connectDepthThreadLocal.get();
        connectDepthThreadLocal.set(currentDepth - 1);

        if (currentDepth == 1) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Checks whether the current instrumentation should start a new extent on getInputStream operation. If there is already an active extent, then do not start one
     * @return
     */
    protected static boolean shouldStartInputStreamExtent() {
        int currentDepth = getInputStreamDepthThreadLocal.get();
        getInputStreamDepthThreadLocal.set(currentDepth + 1);

        if (currentDepth == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether the current instrumentation should end the current extent on getInputStream operation. If this is the active extent being traced, then ends it
     * @return
     */
    protected static boolean shouldEndInputStreamExtent() {
        int currentDepth = getInputStreamDepthThreadLocal.get();
        getInputStreamDepthThreadLocal.set(currentDepth - 1);

        if (currentDepth == 1) {
            return true;
        } else {
            return false;
        }
    }
}
