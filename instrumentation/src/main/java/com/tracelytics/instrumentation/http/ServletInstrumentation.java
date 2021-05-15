package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.instrumentation.solr.SolrFilterInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.XTraceOptionsResponse;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.TraceEventSpanReporter;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.joboe.span.impl.Tracer.SpanBuilder;

import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;


/**
 * Adds instrumentation to HttpServlet classes
 * All HTTP Requests go through this class
 */
public class ServletInstrumentation extends ClassInstrumentation {
    public static final String CLASS_NAME = ServletInstrumentation.class.getName();
    static final String ASYNC_CONTEXT_ATTRIBUTE_NAME = ServletAsyncContext.class.getName();

    private static String traceLayerName;
    
    
    //Flag for whether hide query parameters as a part of the URL or not. By default false 
    private static boolean hideUrlQueryParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.SERVLET) : false;
        
    private static ThreadLocal<List<Span>> servletSpanStackThreadLocal = new ThreadLocal<List<Span>>() {
        @Override
        protected List<Span> initialValue() {
            return new ArrayList<Span>();
        }
    };
    private static ThreadLocal<Boolean> asyncStartedThreadLocal = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("service", new String[] { "javax.servlet.ServletRequest", "javax.servlet.ServletResponse"}, "void", OpType.SERVICE, MethodMatcher.Strategy.STRICT),
            new MethodMatcher<OpType>("service", new String[] { "jakarta.servlet.ServletRequest", "jakarta.servlet.ServletResponse"}, "void", OpType.SERVICE, MethodMatcher.Strategy.STRICT)
    );

    private enum OpType { SERVICE }
 
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        // All requests go through the 'service' method. We need to specify the full signature to find it (use method.getSignature() to dump it out.)
        for (CtMethod serviceMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            modifyService(serviceMethod);
        }

        return true;
    }

    /*  Modifies the service method to add callbacks into our instrumentation code */
    private void modifyService(CtMethod method)
            throws CannotCompileException, NotFoundException {

        // We're casting everything to object because I'm concerned about incompatibilities with different versions
        // of HttpServletRequest, Response, etc. If we add a dependency on the javax.servlet API to our code, it may be different
        // than the one included with the app server, and will probably be loaded with a different class loader, causing exceptions.

        insertBefore(method, CLASS_NAME + ".layerEntry(this, (Object)$1,(Object)$2);", false);
        addErrorReporting(method, "java.lang.Throwable", layerName, classPool);
        insertAfter(method, CLASS_NAME + ".layerExit(this, (Object)$1,(Object)$2);", true, false);
    }



    // These methods are called by the instrumented HttpServlet object
    public static void layerEntry(Object caller, Object oreq, Object oresp) {
        if (!(oreq instanceof HttpServletRequest) || !(oresp instanceof HttpServletResponse)) {
            logger.debug(MessageFormat.format("Skipping instrumentation on servlet [{0}] as the request or/and response object is not Http servlet objects, request [{1}] response [{2}]",
                                              caller.getClass().getName(),
                                              oreq != null ? oreq.getClass().getName() : "null",
                                              oresp != null ? oresp.getClass().getName() : "null"));
            return;
        }
        
        HttpServletRequest request = (HttpServletRequest)oreq;
        HttpServletResponse response = (HttpServletResponse)oresp;

        logger.debug("HttpServlet: Entry: Caller: " + caller.getClass().getName() + " URL: " + getURL(request, false) + " Method: " + request.getMethod() + " Req#: " + response.tlysReqCount() + " SetXTR: " + response.tlysGetXTraceID());
        
        
        
      //if it is the first one in the calling stack. And it's not triggered by an async dispatch 
        if (response.tlysIncReqCount() == 1) {
            ScopeManager.INSTANCE.removeAllScopes(); //clean up as Thread pool service patcher might propagate some span to this thread. This is due to handling of some web server that it kinda pre-spawn thread to handle next incoming requests

            if (!request.tvIsAsyncDispatch()) {
                Map<XTraceHeader, String> xTraceHeaders = extractXTraceHeaders(request);
                
                String layerName = getTraceLayerName();
                SpanBuilder startTraceSpanBuilder = getStartTraceSpanBuilder(layerName, xTraceHeaders, getFullUrl(request), true);

                Span servletSpan = startTraceSpanBuilder.start();
                tracer.activateSpan(servletSpan);
                
                if (servletSpan.context().isSampled()) {
                    // Generate X-Trace HTTP Header
                    // HTTP headers are sent before the end of the service method, so need to know the Op ID for our exit event before we
                    // actually generate it..
                    Metadata exitMetadata = new Metadata(Context.getMetadata());
                    exitMetadata.randomizeOpID();
                    String exitXID = exitMetadata.toHexString();
                    
                    if (!response.containsHeader(XTRACE_HEADER)) { //do NOT update the response header if there's already one - probably added by an earlier/parent layer
                        response.addHeader(XTRACE_HEADER, exitXID);
                    }
                    
                    response.tlysSetXTraceID(exitXID);

                    servletSpan.setSpanPropertyValue(SpanProperty.EXIT_XID, exitXID);
                } else {
                    if (!response.containsHeader(XTRACE_HEADER)) { //do NOT update the response header if there's already one - probably added by an earlier/parent layer
                        response.addHeader(XTRACE_HEADER, servletSpan.context().getMetadata().toHexString());
                    }
                }
                
                //always set these tags regardless of sampling decision as they are useful for both tracing and non tracing operations (metrics)
                servletSpan.setTag("Spec", "ws");
                servletSpan.setTag("HTTPMethod", request.getMethod());
                servletSpan.setTag("HTTP-Host", request.getHeader("host"));
                servletSpan.setTag("Remote-Host", request.getRemoteHost());
                servletSpan.setTag("URL", ServletInstrumentation.getURL(request, true));

                final HttpServletRequest req = request; // must be final in old jdk versions
                ClassInstrumentation.setForwardedTags(servletSpan, new HeaderExtractor<String, String>() {
                    @Override public String extract(String s) {
                        return req.getHeader(s);
                    }
                });

                XTraceOptionsResponse xTraceOptionsResponse = XTraceOptionsResponse.computeResponse(servletSpan);
                if (xTraceOptionsResponse != null) {
                    response.addHeader(X_TRACE_OPTIONS_RESPONSE_KEY, xTraceOptionsResponse.toString());
                }
                servletSpanStackThreadLocal.get().add(servletSpan); //record the base span, if this servlet handling turns into an async operation later via ServletRequest.startAsync(), the span will be passed off to the async handling to finish
            } else {
                //if it's a dispatch, it's unsafe to call getAsyncContext on request, hence we are getting it from attribute instead see https://github.com/librato/joboe/issues/548
                ServletAsyncContext asyncContext = (ServletAsyncContext) request.getAttribute(ASYNC_CONTEXT_ATTRIBUTE_NAME); 
            	if (asyncContext != null) {
                    List<Span> dispatchedSpanStack = (List<Span>) asyncContext.tvGetSpanStack();
            	    if (dispatchedSpanStack != null) {
            	        for (int i = 0; i < dispatchedSpanStack.size(); i ++) {
            	            tracer.activateSpan(dispatchedSpanStack.get(i));
                        }
//            	        Metadata propagatingMetadata = servletScope.span().context().getMetadata();
//                        if (propagatingMetadata != null) {
//                            Context.setMetadata(propagatingMetadata); //TODO this is not necessary but double check just in case
//                        }

                        servletSpanStackThreadLocal.get().addAll(dispatchedSpanStack); //the dispatcher can also have ServletRequest.startAsync() called during the handling
                        asyncContext.tvSetSpanStack(null); //remove the continuation as it has been activated
            	    } else {
            	        logger.warn("Servlet handling async dispatch but could not find the captured span Continuation");
            	    }
            	} else {
            	    logger.warn("Servlet handling async dispatch but could not find the async context from attribute " + ASYNC_CONTEXT_ATTRIBUTE_NAME);
            	}
            }
            

        }
        
        //check recognized framework too
        String frameworkLayer = ServletInstrumentation.getFrameworkLayer(caller);
        
        
        if (frameworkLayer != null && ServletInstrumentation.incFrameworkCounter(response, frameworkLayer) == 1) {
            // Could be a class associated with a framework (example, struts2 filter) which we want to be in a separate layer
            Span frameworkSpan = tracer.buildSpan(frameworkLayer).withReporters(TraceEventSpanReporter.REPORTER).start();
            frameworkSpan.setTag("Servlet-Class", caller.getClass().getName());
            frameworkSpan.setTag("URL", getURL(request, true));

            tracer.activateSpan(frameworkSpan);

            servletSpanStackThreadLocal.get().add(frameworkSpan); //record the scope from framework. As async servlet can be started within the duration of the framewor span
        } else {
            ScopeManager.INSTANCE.activeSpan().log("Servlet-Class", caller.getClass().getName(),
                                          "URL", getURL(request, true));
        }
    }

    /**
     * Gets the top layer name of the trace.
     * @return
     */
    static String getTraceLayerName() {
        if (traceLayerName == null) { //initialize it
            traceLayerName = (String) ConfigManager.getConfig(ConfigProperty.AGENT_LAYER);
            if ("java".equals(traceLayerName)) { //if layer name is the default
                String appServerNameFromTrace = getAppServerNameFromTrace();
                if (appServerNameFromTrace != null) {
                    traceLayerName = appServerNameFromTrace;
                }
            } else if ("jboss".equals(traceLayerName)) { //if layer name is jboss, see if it's actually wildfly
                String appServerNameFromTrace = getAppServerNameFromTrace();
                if ("wildfly".equals(appServerNameFromTrace)) {
                    traceLayerName = "wildfly";
                }
            }
        }
        
        return traceLayerName;
    }

    /**
     * Gets the app server name from the stack trace
     * @return app server name if a match is found, otherwise null
     */
    private static String getAppServerNameFromTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = stackTrace.length - 1; i >= 0 ; i--) { //start from the bottom of the stack
            StackTraceElement frame = stackTrace[i];
            if (frame != null && frame.getClassName() != null) {
                String frameClassName = frame.getClassName();
                if (frameClassName.startsWith("org.eclipse.jetty.") || frameClassName.startsWith("org.mortbay.jetty.")) {
                    return "jetty";
                } else if (frameClassName.startsWith("weblogic.servlet")) {
                    return "weblogic";
                } else if (frameClassName.startsWith("com.ibm.ws.")) {
                    return "websphere";
                } else if (frameClassName.startsWith("org.wildfly.")) {
                    return "wildfly";
                }
            }
        }

        //tomcat and undertow have lower priority as they can be used by other more specific frameworks such as jetty and wildfly
        //in such case we want to detect those more specific frameworks
        for (int i = stackTrace.length - 1; i >= 0 ; i--) { //start from the bottom of the stack
            StackTraceElement frame = stackTrace[i];
            String frameClassName = frame.getClassName();
            if (frame != null && frame.getClassName() != null) {
                if (frameClassName.startsWith("org.apache.tomcat.")) {
                    return "tomcat";
                } else if (frameClassName.startsWith("io.undertow.")) {
                    return "undertow";
                }
                //more server?
            }
        }
        
        return null;
    }

    /**
     * Extract special x-trace headers from the http servlet request 
     * @param req   Http servlet request
     * @return      a Map with key of type XTraceHeader, and value as the Http Request header's value
     * @see         XTraceHeader
     */
    private static Map<XTraceHeader, String> extractXTraceHeaders(HttpServletRequest req) {
        Map<XTraceHeader, String> headers = new HashMap<XTraceHeader, String>();
        
        //cannot iterate from Http request headers, as it is not case sensitive and it might not match to values declared in XTRACE_HTTP_HEADER_KEYS
        for (Entry<String, XTraceHeader> xtraceHeaderEntry : XTRACE_HTTP_HEADER_KEYS.entrySet()) { 
            String httpHeaderValue;
            if ((httpHeaderValue = req.getHeader(xtraceHeaderEntry.getKey())) != null) {
                headers.put(xtraceHeaderEntry.getValue(), httpHeaderValue);
            }
        }
        
        return headers;
    }

    public static void layerExit(Object caller, Object oreq, Object oresp) {
        if (!(oreq instanceof HttpServletRequest) || !(oresp instanceof HttpServletResponse)) {
            logger.debug(MessageFormat.format("Skipping instrumentation on servlet [{0}] as the request or/and response object is not Http servlet objects, request [{1}] response [{2}]",
                                              caller.getClass().getName(),
                                              oreq != null ? oreq.getClass().getName() : "null",
                                              oresp != null ? oresp.getClass().getName() : "null"));
            return;
        }
        
        HttpServletRequest req = (HttpServletRequest)oreq;
        HttpServletResponse resp = (HttpServletResponse)oresp;
    
        logger.debug("HttpServlet: Exit: Caller: " + caller.getClass().getName()  + " URL: " + getURL(req, false) +" Method: " + req.getMethod() +
                    " Status: " + resp.tlysGetStatus() + " Req#: " + resp.tlysReqCount());
        String frameworkLayer = ServletInstrumentation.getFrameworkLayer(caller);
        if (frameworkLayer != null) {
            if (ServletInstrumentation.decFrameworkCounter(resp, frameworkLayer) == 0) {
                Scope closedScope = decorateAndCloseScope(req, resp);
                if (closedScope != null) {
                    servletSpanStackThreadLocal.get().remove(closedScope.span());
                }
            }
        }
             
    
        //last one exiting
        if (resp.tlysDecReqCount() == 0) {
             decorateAndCloseScope(req, resp);
                
            int outstandingScopesCount = ScopeManager.INSTANCE.removeAllScopes(); 
            if (outstandingScopesCount != 0) {
                logger.warn("Expect all spans to be finished but " + outstandingScopesCount + " outstanding scope(s) found");
            }

            asyncStartedThreadLocal.set(false);
            servletSpanStackThreadLocal.get().clear();
            //TODO for now
            Context.clearMetadata();
        }
    }
    
    private static Scope decorateAndCloseScope(HttpServletRequest req, HttpServletResponse resp) {
        Scope scope = ScopeManager.INSTANCE.active();
        if (scope != null) {
            if (!asyncStartedThreadLocal.get()) {
                scope.span().setTag("Status", resp.tlysGetStatus());
                scope.span().finish();
            }

	        scope.close();
        } else {
        	logger.warn("Failed to find span for servlet exit");
        }
        return scope;
    }

    /** 
     * @param checkFlag     whether to perform the flag check on agent.includeUrlQueryParams
     * @return the full URL for the given request (includes query string, unless checkFlag is true and agent.includeUrlQueryParams is present and set to false), or "" if req is null 
     */
    static String getURL(HttpServletRequest req, boolean checkFlag) {
        if (req == null) {
            logger.debug("Null HttpServletRequest, no URL.");
            return ""; // May happen in unit tests, don't crash
        }
        
        if (req.getQueryString() == null || req.getQueryString().equals("")) {
            return req.getRequestURI();
        }
        
        if (checkFlag && hideUrlQueryParams) { //should check the flag agent.hideParams and if the SERVLET module is in there, then do NOT include the query string
            return req.getRequestURI();
        } else {
            return req.getRequestURI() + "?" + req.getQueryString();
        }
    }
    
    static String getFullUrl(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL != null ? requestURL.toString() : null;
        } else {
            return requestURL != null? (requestURL.append('?').append(queryString).toString()) : null;
        }
    }


    /**
     * Increment framework counter associated with response
     * @return count
     */
    static int incFrameworkCounter(HttpServletResponse res, String framework) {
        HashMap map = res.tlysGetFrameworkCounterMap();
        int count = 0;

        if (map.containsKey(framework)) {
            count = (Integer)map.get(framework);
        }

        map.put(framework, ++count);

        if (count == 1) {
            res.tlysGetFrameworkLayerStack().push(framework);
        }

        return count;
    }

    /**
     * Decrement framework counter associated with response
     * @return count
     */
     static int decFrameworkCounter(HttpServletResponse res, String framework) {
        HashMap map = res.tlysGetFrameworkCounterMap();
        int count = 0;

        if (map.containsKey(framework)) {
            count = (Integer)map.get(framework);
            map.put(framework, --count);
            if (count == 0) {
                res.tlysGetFrameworkLayerStack().pop();
            }
        }

        return count;
    }

    static String getLastFrameworkLayer(HttpServletResponse res) {
        String layer = null;

        Stack stack = res.tlysGetFrameworkLayerStack();
        if (stack.isEmpty()) {
            return getTraceLayerName();
        }

        try {
            layer = (String)res.tlysGetFrameworkLayerStack().peek();
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        return layer;
    }
    
    static List<Span> getCurrentServletSpanStack() {
        return servletSpanStackThreadLocal.get();
    }

    static void setAsyncStarted(boolean started) {
         asyncStartedThreadLocal.set(started);
    }

    /**
     * Identifies layer associated with a framework class. Returns null if no layer could
     * be identified
     */
    
    public static String getFrameworkLayer(Object obj) {
        String className = obj.getClass().getName();

        if (className.startsWith("com.opensymphony.xwork2.") || className.startsWith("org.apache.struts2.")) {
            return StrutsActionProxyInstrumentation.LAYER_NAME;
        } else if (className.startsWith("org.springframework.")) {
            return SpringHandlerAdapterInstrumentation.LAYER_NAME;
        } else if (className.startsWith("javax.faces.webapp.") || className.startsWith("jakarta.faces.webapp.")) {
            return JSFActionListenerInstrumentation.LAYER_NAME;
        } else if (className.startsWith("org.apache.solr.")) {
            return SolrFilterInstrumentation.LAYER_NAME;
        } else if (className.startsWith("org.apache.felix.http")) {
            return "felix-http";
        }

        return null;
    }
}



