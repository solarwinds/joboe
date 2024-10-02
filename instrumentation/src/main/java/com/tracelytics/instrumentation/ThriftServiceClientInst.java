package com.tracelytics.instrumentation;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
        
/**
 *  Instruments classes extending org.apache.thrift.TServiceClient
 *  (Client-side Thrift-generated code)
 */
public class ThriftServiceClientInst extends ClassInstrumentation {

    /**
     * Instruments send_ and recv_ methods
     * @param cc
     * @param className
     * @param classBytes
     * @return
     * @throws Exception
     */
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        try {
            CtMethod methods[] = cc.getDeclaredMethods();
            for (CtMethod method: methods) {
                if (method.getName().startsWith("send_")) {
                    modifySendMethod(method, cc);
                } else if (method.getName().startsWith("recv_")) {
                    modifyRecvMethod(method, cc);
                }
            }
        } catch(Exception ex) {
            logger.error("ERROR: Unable to instrument Thrift class " + className + ": " + ex.getMessage());
        }

        return true;
    }

    private void modifySendMethod(CtMethod method, CtClass cc)
        throws CannotCompileException, NotFoundException {
        String layerName = getLayerName(method, cc);
        insertBefore(method, CLASS_NAME + ".layerEntry(\"" + layerName + "\", getOutputProtocol() != null ? getOutputProtocol().getTransport() : null);");
    }

    private void modifyRecvMethod(CtMethod method, CtClass cc)
        throws CannotCompileException, NotFoundException {
        String layerName = getLayerName(method, cc);
        insertAfter(method, CLASS_NAME + ".layerExit(\"" + layerName + "\");", true);
        addErrorReporting(method, "java.lang.Throwable", layerName, classPool);
    }

    /**
     * Generates entry/exit events (called from instrumented code)
     *
     * @param layer
     * @param label
     */
    public static void layerEntry(String layer, Object transportObject) {
        Event event = Context.createEvent();
        event.addInfo("Layer", layer,
                      "Label", "entry");
        
        ClassInstrumentation.addBackTrace(event, 1, Module.THRIFT);
        
        if (transportObject instanceof ThriftTransportWithHost) {
            ThriftTransportWithHost transport = (ThriftTransportWithHost)transportObject;
            if (transport.tvGetHost() != null) {
                event.addInfo("RemoteHost", transport.tvGetHost() + ':' + transport.tvGetPort());
            }
        }
        event.report();
    }
    
    public static void layerExit(String layer) {
        Event event = Context.createEvent();
        event.addInfo("Layer", layer,
                      "Label", "exit");
        
        event.report();
    }

    /**
     * Convert method and class names to Thrift-style layer name
     *   class:client:method
     * @param method
     * @param cc
     * @return
     */
    public static String getLayerName(CtMethod method, CtClass cc) {
        // Get last class name, before inner class $
        String parts[] = cc.getName().split("\\$");
        parts = parts[0].split("\\.");
        String clientClassName = parts[parts.length - 1];
        
        // Get method name, without send_ or recv_
        String methodName = method.getName().substring(5);
        return clientClassName + ":client:" + methodName;    
    }
    
    public static final String CLASS_NAME = ThriftServiceClientInst.class.getName();
}
