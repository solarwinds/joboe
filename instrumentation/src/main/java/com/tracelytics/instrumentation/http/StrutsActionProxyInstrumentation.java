package com.tracelytics.instrumentation.http;

import java.util.HashMap;
import java.util.Map;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;

/**
 * Instruments Struts ActionProxy
 * See http://www.docjar.com/html/api/com/opensymphony/xwork2/ActionProxy.java.html
 *     http://www.docjar.com/html/api/com/opensymphony/xwork2/DefaultActionProxy.java.html
 *
 */
public class StrutsActionProxyInstrumentation extends ClassInstrumentation {
    public static final String CLASS_NAME = StrutsActionProxyInstrumentation.class.getName();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {

        // Add our interface so we can access the modified class during layer
        // entry/exit:
        CtClass iface = classPool.getCtClass("com.tracelytics.instrumentation.http.StrutsActionProxy");
        for (CtClass i : cc.getInterfaces()) {
            if (i.equals(iface)) {
                return true;
            }
        }
        cc.addInterface(iface);

        // We instrument the 'execute' method, which eventually calls into the action class (usually also the 'execute' method, but could be mapped to something else)
        try {
            CtMethod executeMethod = cc.getMethod("execute", "()Ljava/lang/String;");
            if (executeMethod.getDeclaringClass() == cc) {
                modifyExecute(executeMethod);
            }
        } catch(NotFoundException ex) {
            logger.debug("Unable to find execute method in " + cc.getName());
        }

        return true;
    }
    
    public void modifyExecute(CtMethod method) throws CannotCompileException {
        insertBefore(method, CLASS_NAME + ".doExecuteInfo(this);", false);
    }

    /**
     * Logs the action
     *    controller: class with the action method
     *    action: action name on that controller (mapped in struts.xml)
     *    In many cases, action will just be 'execute'.
     *
     * There can actually be multiple actions during a single struts request - they can chain. Though multiple
     * info events are logged, our ETL code only gets the first one.
     *
     * @param actionProxyObj ActionProxy instance
     */
    public static void doExecuteInfo(Object actionProxyObj) {
        StrutsActionProxy actionProxy = (StrutsActionProxy)actionProxyObj;
        Object actionObject = actionProxy.getAction();
        String method = actionProxy.getMethod();

        Span span = ScopeManager.INSTANCE.activeSpan();
        if (span != null) {
            if (span.context().isSampled()) {
                Map<String, String> strutsInfo = new HashMap<String, String>();
                
                strutsInfo.put("Controller", actionObject.getClass().getName());
                strutsInfo.put("Action", actionProxy.getActionName());
                strutsInfo.put("Struts-Action-Namespace", actionProxy.getNamespace());
                if (method != null) {
                    strutsInfo.put("Struts-Action-Method", method);
                }
                
                span.log(strutsInfo);
            }
            
            span.setTracePropertyValue(TraceProperty.CONTROLLER, actionObject.getClass().getName());
            span.setTracePropertyValue(TraceProperty.ACTION, actionProxy.getActionName());
        }
    }
            

    static String LAYER_NAME = "struts";
}
