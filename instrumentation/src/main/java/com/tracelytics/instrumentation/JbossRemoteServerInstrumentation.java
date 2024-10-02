package com.tracelytics.instrumentation;

import java.util.HashMap;
import java.util.Map;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Instruments org.jboss.remoting server class, used for remote EJB calls.
 */
public class JbossRemoteServerInstrumentation extends ClassInstrumentation {
    private static boolean hideInvocationParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.JBOSS) : false;
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        String cls = cc.getName();

        if (cls.equals("org.jboss.remoting.ServerInvoker")) {
            try {
                CtMethod method = cc.getMethod("invoke", "(Lorg/jboss/remoting/InvocationRequest;)Ljava/lang/Object;");
                if (method.getDeclaringClass() == cc) {
                    modifyInvokeMethod(method);
                }
            } catch(NotFoundException ex) {
                logger.debug("Unable to find invoke method", ex);
            }

        }

        return true;
    }


    /*
     Modifies "invoke" to add instrumentation calls
      */
    private void modifyInvokeMethod(CtMethod method)
        throws CannotCompileException {

        insertBefore(method, CLASS_NAME + ".doEntry($1);", false); //context could be from the RequestPayload
        insertAfter(method, CLASS_NAME + ".doExit($1);", true);
    }


    /**
     * Entry into remote invocation server layer
     */
    public static void doEntry(Object objReq) {
        JbossRemoteInvocationRequest req = (JbossRemoteInvocationRequest)objReq;
        Event event = null;
        String xtrace = null;
        
        if (req.getRequestPayload() != null) {
            xtrace = (String) req.getRequestPayload().get(JbossRemoteClientInstrumentation.XTRACE_ID);
        }

        if (xtrace != null) {
            try {
                Context.setMetadata(xtrace);
                event = Context.createEvent();
            } catch(OboeException ex) {
                logger.debug("Invalid X-Trace ID received: " + xtrace, ex);
            }
        } else {
            // XXX: Would we ever want to start traces from here? Could be useful in cases where the EJB client
            // (web server or perhaps a native client) is not instrumented.
            event = Context.createEvent();
        }

        event.addInfo("Layer", ConfigManager.getConfig(ConfigProperty.AGENT_LAYER) + LAYER_NAME,
                      "Label", "entry",
                      "Invocation_Subsystem", req.getSubsystem(),
                      "Invocation_Params_Class", req.getParameter().getClass().getName());
        
        if (!hideInvocationParams) {
            event.addInfo("Invocation_Params", req.getParameter().toString());
        }

        // Extract class / method / parameters from remote invocation
        if (req.getParameter() instanceof JbossInvocationBase) {
            JbossRemoteClientInstrumentation.addMethodInfo( (JbossInvocationBase) req.getParameter(), event, false);
        }

        event.report();
    }

    /**
     * Exit from remote invocation client layer
     */
    public static void doExit(Object objReq) {
        JbossRemoteInvocationRequest req = (JbossRemoteInvocationRequest)objReq;
        
        Event event = Context.createEvent();
        event.addInfo("Layer", ConfigManager.getConfig(ConfigProperty.AGENT_LAYER) + LAYER_NAME,
                      "Label", "exit");
        
        event.report();

        // Return XTrace ID to caller, processed on the client side by JBossRemoteInvocationClientInstrumentation
        Map payload = req.getReturnPayload();
        if (payload == null) {
            payload = new HashMap();
            req.setReturnPayload(payload);
        }

        payload.put(JbossRemoteClientInstrumentation.XTRACE_ID, Context.getMetadata().toHexString());
        Context.clearMetadata();
    }

    static String CLASS_NAME = "com.tracelytics.instrumentation.JbossRemoteServerInstrumentation";
    static String LAYER_NAME = "-remoteserver";
}
