package com.tracelytics.instrumentation.http;

import java.net.HttpURLConnection;
import java.net.URL;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.ext.javassist.expr.ExprEditor;
import com.tracelytics.ext.javassist.expr.MethodCall;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/*
Instruments JBoss Remote Invocation HTTP Client

For source to the client, see http://docs.jboss.org/jbossas/javadoc/4.0.2/org/jboss/invocation/http/interfaces/Util.java.html for method source

The original intent was to modify the underlying java.net.HttpURLConnection class. However,
Java class loader / security restrictions prevent this (ClassDefNotFoundError exceptions when trying to call back in
to our instrumentation code.)

*/
public class JbossHttpInvocationClientInstrumentation extends ClassInstrumentation {

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        String cls = cc.getName();

        if (cls.equals("org.jboss.invocation.http.interfaces.Util")) {
            // Find "invoke":
            try {
                CtMethod method = cc.getMethod("invoke", "(Ljava/net/URL;Lorg/jboss/invocation/Invocation;)Ljava/lang/Object;");
                if (method.getDeclaringClass() == cc) {
                    modifyInvokeMethod(method);
                }
            } catch(NotFoundException ex) {
                logger.debug("Unable to find invoke method", ex);
            }
            
        } else if(cls.equals("org.jboss.invocation.Invocation")) {
            // Tag Invocation with an interface so we can access it during layer entry/exit:
            CtClass iface = classPool.getCtClass("com.tracelytics.instrumentation.http.JbossInvocation");

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

        insertBefore(method, CLASS_NAME + ".doEntry($1, $2);");

        // Edit the method: we're not just doing a before/after : http://www.csg.is.titech.ac.jp/~chiba/javassist/tutorial/tutorial2.html
        method.instrument(new ExprEditor() {
            
            public void edit(MethodCall m) throws CannotCompileException {
                boolean addedHeader = false, addedExit = false;

                if (m.getClassName().equals("java.net.HttpURLConnection")) {
                    if (m.getMethodName().equals("setRequestMethod") && !addedHeader) {
                        // Before request is sent:
                        insertBeforeMethodCall(m, CLASS_NAME + ".doAddXTraceHeader($0);", false);
                        addedHeader = true;

                    } else if (m.getMethodName().equals("getHeaderField") && !addedExit) {
                        // After response received:
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
    public static void doEntry(URL url, Object objInvoke) {
        JbossInvocation invoke = (JbossInvocation)objInvoke;
        Event event = Context.createEvent();

        event.addInfo("Layer", ConfigManager.getConfig(ConfigProperty.AGENT_LAYER) + LAYER_NAME,
                      "Label", "entry",
                      "ClientURL", url.toString());

        if (invoke.getMethod() != null) {
            event.addInfo("InvokeClass", invoke.getMethod().getDeclaringClass().getName());
            event.addInfo("InvokeMethod", invoke.getMethod().getName());
        }

        // XXX: there may be other interesting information in the Invocation object

        ClassInstrumentation.addBackTrace(event, 1, Module.JBOSS);
        event.report();
    }

    /**
     * Adds XTrace header to HTTP client
     */
    public static void doAddXTraceHeader(HttpURLConnection http) {
        Metadata md = Context.getMetadata();
        if (md.isValid()) {
            http.addRequestProperty(ServletInstrumentation.XTRACE_HEADER, md.toHexString());
        }
    }

    /**
     * Exit from remote invocation client layer
     */
    public static void doExit(HttpURLConnection http) {
        String xTrace = http.getHeaderField(ServletInstrumentation.XTRACE_HEADER);
        Event event = Context.createEvent();

        // HTTP XTrace header is only present if remote server is instrumented
        if (xTrace != null) {
            event.addEdge(xTrace);
        }

        event.addInfo("Layer", ConfigManager.getConfig(ConfigProperty.AGENT_LAYER) + LAYER_NAME,
                      "Label", "exit");

        event.report();
    }


    static String CLASS_NAME = "com.tracelytics.instrumentation.http.JbossHttpInvocationClientInstrumentation";
    static String LAYER_NAME = "-jbossclient";
}
