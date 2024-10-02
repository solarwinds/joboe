package com.tracelytics.instrumentation.ejb;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Instruments <code>org.jboss.ejb.client.EJBClientInvocationContext</code> and serves 3 purposes:
 * 1. When the request is ready to be sent, inject the x-trace id as part of the attachments such that the ejb server instrumentation can continue the trace
 * 2. Tag the Invocation Context itself with x-trace id of the last reported event after sending off an asynchronous request. This is necessary to build the async exit event as 
 * exit happens in a different thread which still has a reference to the same Invocation Context
 * 3. Creates the exit event for asynchronous operation upon "resultReady" is invoked. Take note that this is the safest point to exit as this is guaranteed to be invoked
 * even for "fire-and-forget" operation. Take note that at "resultReady" the response inputstream is still not read, therefore it is impossible to parse the x-trace id
 * from the response attachment like in synchronous operation. However, the rendering of the trace appears to be correct even with 1 missing edge
 * 
 * 
 * @author Patson Luk
 *
 */
public class JbossEjbInvocationContextInstrumentation extends ClassInstrumentation {
    private static final String LAYER_NAME = "-jbossclient";
    private static String CLASS_NAME = JbossEjbInvocationContextInstrumentation.class.getName();

    private enum MethodType { SEND_REQUEST, RESULT_READY }
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(new MethodMatcher<MethodType>("sendRequest", new String[] {}, "void", MethodType.SEND_REQUEST),
                                                                              new MethodMatcher<MethodType>("resultReady", new String[] {}, "void", MethodType.RESULT_READY));
    
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        for (Entry<CtMethod, MethodType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            MethodType type = methodEntry.getValue();
            if (type == MethodType.SEND_REQUEST) {
                insertBefore(method, CLASS_NAME + ".addContext(getContextData());", false);
                insertAfter(method, CLASS_NAME + ".tagContext(this);", true);
            } else if (type == MethodType.RESULT_READY) { 
                insertAfter(method, CLASS_NAME + ".asyncExit(this);", true, false); //create exit event for async operations, this is the safest place as this is guaranteed to be called even for fire-and-forget requests (that does not call get() on the Future object)
            }
            
        }
        
        addTvContextObjectAware(cc);
        
        return true;
    }

    /**
     * Adds the x-trace id to the attachment map such that it will be propagated to the server side properly 
     * @param contextData   attachment map 
     */
    public static void addContext(Map<String, Object> contextData) {
        if (Context.isValid()) {
            if (JbossEjbContext.getAsyncContext() != null) { //async operation, take the async context
                contextData.put(ServletInstrumentation.XTRACE_HEADER, JbossEjbContext.getAsyncContext().toHexString());
            } else {
                contextData.put(ServletInstrumentation.XTRACE_HEADER, Context.getMetadata().toHexString());
            }
        }
    }
    
    /**
     * Tag the Invocation Context itself with x-trace id of the last reported event after sending off an asynchronous request. This is necessary to build the async exit event as 
     * exit happens in a different thread which still has a reference to the same Invocation Context
     * @param invocationContextObject
     */
    public static void tagContext(Object invocationContextObject) {
        if (JbossEjbContext.getAsyncContext() != null && invocationContextObject instanceof TvContextObjectAware) { //then tag current context, such that an exit event will be create on resultReady
            ((TvContextObjectAware)invocationContextObject).setTvContext(JbossEjbContext.getAsyncContext());
        }
    }
    
    /**
     * Creates the exit event for asynchronous operation upon "resultReady" is invoked. Take note that this is the safest point to exit as this is guaranteed to be invoked
     * even for "fire-and-forget" operation. Take note that at "resultReady" the response inputstream is still not read, therefore it is impossible to parse the x-trace id
     * from the response attachment like in synchronous operation. However, the rendering of the trace appears to be correct even with 1 missing edge
     * @param invocationContextObject
     */
    public static void asyncExit(Object invocationContextObject) {
        if (invocationContextObject instanceof TvContextObjectAware) {
            TvContextObjectAware invocationContext = (TvContextObjectAware) invocationContextObject;
            if (invocationContext.getTvContext() != null) { //then this was started as async invocation, should end the extent here
                Context.setMetadata(invocationContext.getTvContext());
                Event event = Context.createEvent();

                event.addInfo("Layer", ConfigManager.getConfig(ConfigProperty.AGENT_LAYER) + LAYER_NAME,
                              "Label", "exit");
                
                //unfortunately for async event, we cannot parse the response x-trace id, as at this point we cannot parse the inputstream before the ResultProducer does
                //and the ResultProducer will only be invoked if "get" is called on the Future object, which cannot be guaranteed in "fire-and-forget" operations
                event.report();
                
                invocationContext.setTvContext(null); //clear the context
            }
        } else {
            logger.warn("EJBClientInvocationContext of class [" + invocationContextObject.getClass().getName() + "] is not tagged as " + TvContextObjectAware.class.getName() + ". Cannot end the async extent");
        }
        
    }
}
