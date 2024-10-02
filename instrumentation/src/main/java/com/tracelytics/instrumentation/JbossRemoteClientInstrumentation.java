package com.tracelytics.instrumentation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.ext.javassist.expr.ExprEditor;
import com.tracelytics.ext.javassist.expr.MethodCall;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Instruments org.jboss.remoting client classes, used for remote EJB calls.
 */
public class JbossRemoteClientInstrumentation extends ClassInstrumentation {
    private static boolean hideInvocationParams = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.JBOSS) : false;
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        String cls = cc.getName();

        if (cls.equals("org.jboss.remoting.MicroRemoteClientInvoker")) {
            try {
                CtMethod method = cc.getMethod("invoke", "(Lorg/jboss/remoting/InvocationRequest;)Ljava/lang/Object;");
                if (method.getDeclaringClass() == cc) {
                    modifyInvokeMethod(method);
                }
            } catch(NotFoundException ex) {
                logger.debug("Unable to find invoke method", ex);
            }

        } else if (cls.equals("org.jboss.remoting.InvocationRequest")) {
           // Tag Request with an interface so we can access it during layer entry/exit:
            CtClass iface = classPool.getCtClass("com.tracelytics.instrumentation.JbossRemoteInvocationRequest");

            for(CtClass i : cc.getInterfaces()) {
                if (i.equals(iface)) {
                    return true; // already tagged
                }
            }

            cc.addInterface(iface);

        } else if (cls.equals("org.jboss.remoting.InvocationResponse")) {
           // Tag Response with an interface so we can access it during layer entry/exit:
            CtClass iface = classPool.getCtClass("com.tracelytics.instrumentation.JbossRemoteInvocationResponse");

            for(CtClass i : cc.getInterfaces()) {
                if (i.equals(iface)) {
                    return true; // already tagged
                }
            }

            cc.addInterface(iface);
        } else if (cls.equals("org.jboss.aop.joinpoint.InvocationBase")) {
            // Tag so we can get access to remote invocation parameters
            CtClass iface = classPool.getCtClass("com.tracelytics.instrumentation.JbossInvocationBase");

            for(CtClass i : cc.getInterfaces()) {
                if (i.equals(iface)) {
                    return true; // already tagged
                }
            }

            CtMethod m = CtNewMethod.make( "public Object tlysGetMetaData() { return (Object)this.getMetaData(); }", cc);
            cc.addMethod(m);

            cc.addInterface(iface);

        } else if (cls.equals("org.jboss.aop.joinpoint.MethodInvocation")) {
            // Tag so we can get access to remote invocation method
            CtClass iface = classPool.getCtClass(JbossMethodInvocation.class.getName());

            for(CtClass i : cc.getInterfaces()) {
                if (i.equals(iface)) {
                    return true; // already tagged
                }
            }

            cc.addInterface(iface);
        } else if (cls.equals("org.jboss.aop.metadata.SimpleMetaData")) {

            // Tag so we can get access to remote invocation parameters
            CtClass iface = classPool.getCtClass("com.tracelytics.instrumentation.JbossSimpleMetaData");

            for(CtClass i : cc.getInterfaces()) {
                if (i.equals(iface)) {
                    return true; // already tagged
                }
            }

            cc.addInterface(iface);
        }

        return true;
    }


    /*
     Modifies "invoke" to add instrumentation calls
      */
    private void modifyInvokeMethod(CtMethod method)
        throws CannotCompileException {

        insertBefore(method, CLASS_NAME + ".doEntry($1);", false);

        // Insert exit after we get the remote response
        method.instrument(new ExprEditor() {

            public void edit(MethodCall m) throws CannotCompileException {
                boolean addedExit = false;

                if (m.getClassName().equals("org.jboss.remoting.InvocationResponse")) {
                    if (m.getMethodName().equals("getResult") && !addedExit) {
                        insertBeforeMethodCall(m, CLASS_NAME + ".doExit($0);");
                        addedExit = true;
                    }
                }
            }
        });

    }


    /**
     * Entry into remote invocation client layer
     */
    public static void doEntry(Object objReq) {
        Metadata md = Context.getMetadata();
        JbossRemoteInvocationRequest req = (JbossRemoteInvocationRequest)objReq;
        if (md.isSampled()) {
            Event event = Context.createEvent();
    
            event.addInfo("Layer",  (String) ConfigManager.getConfig(ConfigProperty.AGENT_LAYER) + LAYER_NAME,
                          "Label", "entry",
                          "Invocation_Subsystem", req.getSubsystem(),
                          "Invocation_Params_Class", req.getParameter().getClass().getName());
            
            if (!hideInvocationParams) {
                event.addInfo("Invocation_Params", req.getParameter().toString());
            }
    
            // Extract class / method / parameters from remote invocation
            if (req.getParameter() instanceof JbossInvocationBase) {
                addMethodInfo( (JbossInvocationBase) req.getParameter(), event, true);
            }
    
            ClassInstrumentation.addBackTrace(event, 1, Module.JBOSS);
            event.report();
        }

        if (md.isValid()) {
            // Payload contains name/value pairs, according to the org.jboss.remoting source...
            Map payload = req.getRequestPayload();
            if (payload == null) {
                payload = new HashMap();
                req.setRequestPayload(payload);
            }
            
            payload.put(XTRACE_ID, md.toHexString());
        }

    }

    /**
     * Exit from remote invocation client layer
     */
    public static void doExit(Object objResp) {
        JbossRemoteInvocationResponse resp = (JbossRemoteInvocationResponse)objResp;
        Event event = Context.createEvent();

        // XTrace ID is only present if remote server is instrumented
        Map payload = resp.getPayload();
        String xTrace = null;
        
        if (payload != null) {
            xTrace = (String) payload.get(XTRACE_ID);
        
            if (xTrace != null) {
                event.addEdge(xTrace); 
            }
        }

        event.addInfo("Layer",  (String) ConfigManager.getConfig(ConfigProperty.AGENT_LAYER) + LAYER_NAME,
                      "Label", "exit");

        event.report();
    }

    /**
     * Extracts remote class, method, and host from remote invocation parameters. Take note that we previously check SessionInvocation for the method but in certain cases
     * Remote calls (preparation of the calls) are made w/o such a value in the MetaData. Therefore a fall back was added to check whether the invocation was tagged as
     * JbossMethodInvocation, which we can extract method information from
     * 
     * @param inv
     * @param event
     * @param isClient
     */
    public static void addMethodInfo(JbossInvocationBase inv, Event event, boolean isClient) {
        boolean addedMethod = false;

        try {
            JbossSimpleMetaData md = (JbossSimpleMetaData) inv.tlysGetMetaData();

            // Extract class and method for remote object
            Object invMethod = md.getMetaData("SessionInvocation", "InvokedMethod");

            if (invMethod != null && invMethod.getClass().getName().equals("org.jboss.ejb3.common.lang.SerializableMethod")) {

                // We don't modify SerializableMethod class: modifying a serializable class could cause problems
                // with de-serializing it if the other end hasn't been instrumented, so I am hesitant.
                // See http://grepcode.com/file/repository.jboss.org/maven2/org.jboss.ejb3/jboss-ejb3-common/1.0.0/org/jboss/ejb3/common/lang/SerializableMethod.java

                String invoked = invMethod.toString(); // FORMAT: actualClass: declaringClass.method(args)
                String parts[] = invoked.split(":");

                if (parts.length >= 2) {
                    String invokedClass = parts[0];
                    String declaredClassMethodArgs = parts[1];

                    // Strip off args:
                    int firstParen = declaredClassMethodArgs.indexOf('(');

                    if (firstParen != -1) {
                        // Find method
                        String classMethod = declaredClassMethodArgs.substring(0, firstParen);
                        int lastDot = classMethod.lastIndexOf('.');

                        if (lastDot != -1) {
                            String method = classMethod.substring(lastDot + 1);

                            if (isClient) {
                                // Necessary so ETL recognizes this as a service call:
                                event.addInfo("RemoteProtocol", "EJB",
                                              "RemoteController", invokedClass,
                                              "RemoteAction", method,
                                              "Spec", "rsc",
                                              "IsService", Boolean.TRUE);
                            }

                            // More sensibly named copies of the same class and method:
                            event.addInfo("RemoteClass", invokedClass,
                                          "RemoteMethod", method);

                            addedMethod = true;
                        }
                    }
                }
            } else if (inv instanceof JbossMethodInvocation) { //if SessionInvocation is not found that we check whether it is a JBossMethodInvocation. If so, extract method info from it
                Method invokedMethod = ((JbossMethodInvocation)inv).getMethod();
                if (invokedMethod != null && invokedMethod.getDeclaringClass() != null) {
                    if (isClient) {
                     // Necessary so ETL recognizes this as a service call:
                        event.addInfo("RemoteProtocol", "EJB",
                                      "RemoteController", invokedMethod.getDeclaringClass().getName(),
                                      "RemoteAction", invokedMethod.getName(),
                                      "Spec", "rsc",
                                      "IsService", Boolean.TRUE);
                    }
                    
                    // More sensibly named copies of the same class and method:
                    event.addInfo("RemoteClass", invokedMethod.getDeclaringClass().getName(),
                                  "RemoteMethod", invokedMethod.getName());

                    addedMethod = true;
                }
            }
            if (addedMethod && isClient) {
                String remoteHost = getRemoteHost(md);
                
                if (remoteHost != null) {
                    event.addInfo("RemoteHost", remoteHost);
                }
                
            }
        } catch (Throwable ex) {
            logger.debug("Error extracting remote method info", ex);
        }
    }

    /**
     * Find remote host
     * @param md
     * @return
     * 
     * @see http://grepcode.com/file/repository.jboss.org/nexus/content/repositories/releases/org.jboss.remoting/jboss-remoting/2.5.2.SP2/org/jboss/remoting/InvokerLocator.java
     */
    private static String getRemoteHost(JbossSimpleMetaData md) {
        Object remotingInvoker = md.getMetaData("REMOTING", "INVOKER_LOCATOR");
        if (remotingInvoker != null && remotingInvoker.getClass().getName().equals("org.jboss.remoting.InvokerLocator")) {
            String remoteStr = remotingInvoker.toString(); // FORMAT: RemotingInvoker [uri]

            int startPos = remoteStr.indexOf('[');
            int endPos = remoteStr.indexOf(']');

            if (startPos != -1 && endPos > startPos) {
                String hostURI = remoteStr.substring(startPos + 1, endPos - 1);

                // URI is like socket://192.168.10.1:9999. We just want the host...
                startPos = hostURI.indexOf("://");
                if (startPos != -1) {
                    endPos = hostURI.lastIndexOf(":");
                    if (endPos == -1 || endPos <= startPos) {
                        endPos = hostURI.length(); // no port
                    }

                    String remoteHost = hostURI.substring(startPos + 3, endPos);
                    return remoteHost;
                }
            }
        }
        return null;
    }

    static String CLASS_NAME = "com.tracelytics.instrumentation.JbossRemoteClientInstrumentation";
    static String LAYER_NAME = "-jbossclient";
    static String XTRACE_ID = "TLYS_X-TRACE_ID";
}
