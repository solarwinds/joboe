package com.tracelytics.instrumentation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.EventValueConverter;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Instruments Jboss 7/8 EJB server side handling
 * 
 * @author Patson Luk
 *
 */
public class JbossRemoteServerHandlerInstrumentation extends ClassInstrumentation {
    private enum Version { JBOSS_7, JBOSS_8 }
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Version>> methodMatchers = Arrays.asList(
         new MethodMatcher<Version>("invokeMethod", new String[] { "java.lang.Object", "java.lang.reflect.Method", "java.lang.Object[]", "java.lang.Object", "java.util.Map"}, "java.lang.Object", Version.JBOSS_7),//jboss 7
         new MethodMatcher<Version>("invokeMethod", new String[] { "short", "java.lang.Object", "java.lang.reflect.Method", "java.lang.Object[]", "java.lang.Object", "java.util.Map"}, "java.lang.Object", Version.JBOSS_8)); //jboss 8
    private static EventValueConverter eventValueConverter =  new EventValueConverter();
    private static boolean hideInvocationParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.JBOSS) : false;
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (Entry<CtMethod, Version> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            Version version = entry.getValue();
            
            String methodToken;
            String parametersToken;
            String attachmentsToken;
            
            if (version == Version.JBOSS_7) {
                methodToken = "$2";
                parametersToken = "$3";
                attachmentsToken = "$5";
            } else {
                methodToken = "$3";
                parametersToken = "$4";
                attachmentsToken = "$6";
            }
            
            insertBefore(method, CLASS_NAME + ".doEntry(" + methodToken + ", " + parametersToken + ", " +  attachmentsToken + ");", false); //context might be coming in from attachments 
            insertAfter(method, CLASS_NAME + ".doExit(" + attachmentsToken + ");", true);
        }
        
        return true;
    }

    public static void doEntry(Method remoteMethod, Object[] parameters, Map<String, Object> attachments) {
        String xTrace = null;
        if (attachments != null && 
            attachments.containsKey(ServletInstrumentation.XTRACE_HEADER) && 
            attachments.get(ServletInstrumentation.XTRACE_HEADER) instanceof String) {
            xTrace = (String) attachments.get(ServletInstrumentation.XTRACE_HEADER);
        }
        
        if (xTrace != null) {
            try {
                Context.setMetadata(xTrace);
            } catch(OboeException ex) {
                logger.warn("Invalid X-Trace ID received: " + xTrace, ex);
            }
        } else {
            logger.debug("Do not continue trace on this EJB remote server as there is no active trace from client");
            return;
        }
        
        if (Context.getMetadata().isSampled()) {
            Event event = Context.createEvent();
            
            String appServerName = (String) ConfigManager.getConfig(ConfigProperty.AGENT_LAYER);
            if ("java".equals(appServerName)) {
                appServerName = "jboss"; //if cannot recognize the app server, use jboss
            }
            
            event.addInfo("Layer", appServerName + LAYER_NAME,
                          "Label", "entry");
                          
            if (!hideInvocationParams && parameters != null && parameters.length > 0) { 
                List<Object> convertedParams = new ArrayList<Object>();
                for (Object parameter : parameters) {
                    convertedParams.add(eventValueConverter.convertToEventValue(parameter));
                }
    
                event.addInfo("Invocation_Params", convertedParams.toArray());
            }
            
            if (remoteMethod != null) {
                if (remoteMethod.getDeclaringClass() != null) {
                    event.addInfo("RemoteClass", remoteMethod.getDeclaringClass().getName());
                }
                event.addInfo("RemoteMethod", remoteMethod.getName());
            }
    
            event.report();
        }
    }

    /**
     * Set the x-trace id to the attachments returned, this will allow ejb client to build edge pointing to the server exit
     * @param attachments
     */
    public static void doExit(Map<String, Object> attachments) {
        Event event = Context.createEvent();
        
        String appServerName =  (String) ConfigManager.getConfig(ConfigProperty.AGENT_LAYER);
        if ("java".equals(appServerName)) {
            appServerName = "jboss"; //if cannot recognize the app server, use jboss
        }
        
        event.addInfo("Layer", appServerName + LAYER_NAME,
                      "Label", "exit");
        
        event.report();

        if (attachments != null) {
            attachments.put(ServletInstrumentation.XTRACE_HEADER, Context.getMetadata().toHexString());
        }
        
        Context.clearMetadata();
    }

    static String CLASS_NAME = JbossRemoteServerHandlerInstrumentation.class.getName();
    static String LAYER_NAME = "-remoteserver";
}
