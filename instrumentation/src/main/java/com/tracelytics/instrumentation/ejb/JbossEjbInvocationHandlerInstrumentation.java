package com.tracelytics.instrumentation.ejb;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.EventValueConverter;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Instruments the <code>org.jboss.ejb.client.EJBInvocationHandler</code> which is the entry and exit point of ejb operations.
 * 
 * Take note that for asynchronous operation, this will not be the exit point as it returns before the operation is finishes. For asynchronous operation exit event,
 * please refer to {@link JbossEjbInvocationContextInstrumentation}
 * 
 * @author Patson Luk
 *
 */
public class JbossEjbInvocationHandlerInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = JbossEjbInvocationHandlerInstrumentation.class.getName();
    private static String LAYER_NAME = "-jbossclient";
    private static boolean hideInvocationParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.JBOSS) : false;

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(new MethodMatcher<Object>("doInvoke", 
            new String[] { "java.lang.Object",
                           "java.lang.reflect.Method",
                           "java.lang.Object[]" }, 
                           "java.lang.Object"));
    
    private static EventValueConverter converter = new EventValueConverter(); //used to convert value compatible to json
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        String isAsyncToken;
        try {
            //look up the private field first, it's not very reliable as it's declared as private
            cc.getDeclaredField("async");
            isAsyncToken = "async";
        } catch (NotFoundException e) {
            try {
            //try to find the method then, which is only available in jboss-ejb-client 2.0+
            cc.getDeclaredMethod("isAsyncHandler");
            isAsyncToken = "isAsyncHandler()";
            } catch (NotFoundException e2) {
                logger.warn("Cannot find async field nor isAsyncHandler in [" + className + "]. Cannot instrument this class for EJB events");
                return false;
            }
        }
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, CLASS_NAME + ".doEntry(" + isAsyncToken + ", $2, $3);");
            insertAfter(method, CLASS_NAME + ".doExit(" + isAsyncToken + ");", true);
        }
        
        return true;
    }

    /**
     * Creates entry event for EJB invocation
     * @param isAsync
     * @param method
     * @param parameters
     */
    public static void doEntry(boolean isAsync, Method method, Object[] parameters) {
        Event event = Context.createEvent();
        event.addInfo("Layer", ConfigManager.getConfig(ConfigProperty.AGENT_LAYER) + LAYER_NAME,
                      "Label", "entry");
        
        if (method != null) {
         // Necessary so ETL recognizes this as a service call: (copied from JbossRemoteClientInstrumentation)
            event.addInfo("RemoteProtocol", "EJB",
                          "RemoteController", method.getDeclaringClass().getName(),
                          "RemoteAction", method.getName(),
                          "Spec", "rsc",
                          "IsService", true);
            
            event.addInfo("RemoteClass", method.getDeclaringClass().getName());
            event.addInfo("RemoteMethod", method.getName());
        }
        
        if (!hideInvocationParams && parameters != null && parameters.length > 0) {
            List<Object> convertedParameters = new ArrayList<Object>();
            for (Object parameter : parameters) {
                convertedParameters.add(converter.convertToEventValue(parameter));
            }
            event.addInfo("Invocation_Params", convertedParameters.toArray());
        }

        ClassInstrumentation.addBackTrace(event, 1, Module.JBOSS);
        
        Metadata existingContext = null;
        Metadata clonedContext = null;
        if (isAsync) { //create a clone of existing context, so the operation appears as a fork (avoid inline)
            existingContext = Context.getMetadata();
            clonedContext = new Metadata(existingContext);
            
            Context.setMetadata(clonedContext);
            
            event.setAsync();
        }
        
        event.report();
        
        if (isAsync) { //restore the existing context, so other non jboss ejb events continue on in the main flow
            Context.setMetadata(existingContext);
            JbossEjbContext.setAsyncContext(clonedContext); //set the async context so other events for ejb can continue on the same forked extent
        }
    }

    /**
     * Creates an exit event for the ejb opreation. Take note that only synchronous ejb operations will exit here. For asynchronous event, please refer to {@link JbossEjbInvocationContextInstrumentation}
     * @param isAsync
     */
    public static void doExit(boolean isAsync) {
        if (isAsync) {
            JbossEjbContext.setAsyncContext(null); //no longer active, finished the invocation
        } else {
            Event event = Context.createEvent();

            event.addInfo("Layer", ConfigManager.getConfig(ConfigProperty.AGENT_LAYER) + LAYER_NAME,
                          "Label", "exit");
            
            if (JbossEjbContext.getResponseXTraceId() != null) {
                event.addEdge(JbossEjbContext.getResponseXTraceId());
                JbossEjbContext.setResponseXTraceId(null); //edge built, clear the context
            }
    
            event.report();
        }
    }
}
