package com.tracelytics.instrumentation.ejb;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Reports the endpoint used in the operation. Take note that we cannot instrument this in higher level such as {@link JbossEjbInvocationHandlerInstrumentation} as the
 * endpoint is undetermined there. Only when the receiver is being selected and used would the actual endpoint be revealed
 * 
 * @author Patson Luk
 *
 */
public class JbossEjbReceiverInstrumentation extends ClassInstrumentation {
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<Object>> constructorMatchers = Arrays.asList(new ConstructorMatcher<Object>(new String[] {"org.jboss.remoting3.Connection"}));

    private static String CLASS_NAME = JbossEjbReceiverInstrumentation.class.getName();
    private static String LAYER_NAME = "-jbossclient";
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(new MethodMatcher<Object>("processInvocation", 
            new String[] { "org.jboss.ejb.client.EJBClientInvocationContext", "org.jboss.ejb.client.EJBReceiverInvocationContext" }, "void"));
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        cc.addField(CtField.make("private Object tvConnection;", cc));
   
        //Create our own field to keep trace of the tvConnection as using the private field from the actual implementation is unreliable
        for (CtConstructor constructor : findMatchingConstructors(cc, constructorMatchers).keySet()) {
            insertAfter(constructor, "tvConnection = $1;", true);
        }
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertAfter(method, CLASS_NAME + ".reportEndpoint(tvConnection);", true);
        }
        
        return true;
    }

    public static void reportEndpoint(Object connectionObject) {
        if (connectionObject instanceof JbossConnection) {
            JbossConnection connection = (JbossConnection) connectionObject;
            
            if (connection.tvGetHost() != null) {
                Metadata existingContext = null;
                if (JbossEjbContext.getAsyncContext() != null) {
                    existingContext = Context.getMetadata();
                    Context.setMetadata(JbossEjbContext.getAsyncContext());
                }
                
                Event event = Context.createEvent();
                event.addInfo("Layer",  ConfigManager.getConfig(ConfigProperty.AGENT_LAYER) + LAYER_NAME,
                              "Label", "info",
                              "RemoteHost", connection.tvGetHost() + ":" + connection.tvGetPort());
                
                if (connection.getRemoteEndpointName() != null) {
                    event.addInfo("RemoteEndpointName", connection.getRemoteEndpointName());
                }
                
                event.report();
                
                if (existingContext != null) { //restore the existing context so it appears as a fork
                    Context.setMetadata(existingContext);
                }
            }
        }
    }
}
