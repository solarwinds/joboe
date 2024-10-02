package com.tracelytics.instrumentation;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 *  Instruments classes extending org.apache.thrift.ProcessFunction
 *  (Server-side Thrift-generated code)
 */
public class ThriftProcessFunctionInst extends ClassInstrumentation {

    /**
     * Instruments getResult methods
     * @param cc
     * @param className
     * @param classBytes
     * @return
     * @throws Exception
     */
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        if (cc.getName().equals("org.apache.thrift.ProcessFunction")) {
            // Don't instrument the base class
            return false;
        }

        try {
            CtMethod methods[] = cc.getDeclaredMethods();
            for (CtMethod method: methods) {
                if (method.getName().equals("getResult") &&
                    !method.getSignature().equals("(Ljava/lang/Object;Lorg/apache/thrift/TBase;)Lorg/apache/thrift/TBase;")) {
                    modifyGetResultMethod(method, cc);
                }
            }
        } catch(Exception ex) {
            logger.error("ERROR: Unable to instrument Thrift class " + className + ": " + ex.getMessage());
        }

        return true;
    }

    private void modifyGetResultMethod(CtMethod method, CtClass cc)
        throws CannotCompileException, NotFoundException {
        String layerName = getLayerName(method, cc);
        insertBefore(method, CLASS_NAME + ".logMethod(\"" + layerName + "\",\"entry\");");
        insertAfter(method, CLASS_NAME + ".logMethod(\"" + layerName + "\",\"exit\");", true);
    }


    /**
     * Generates entry/exit events (called from instrumented code)
     * @param layer
     * @param label
     */
    public static void logMethod(String layer, String label) {
        Event event = Context.createEvent();
        event.addInfo("Layer", layer,
                      "Label", label);

        event.report();
    }

    /**
     * Convert method and class names to Thrift-style layer name
     *   class:server:method
     * @param method
     * @param cc
     * @return
     */
    public static String getLayerName(CtMethod method, CtClass cc) {
        // Server "methods" are each in an inner class named class$Processor$method, example:
        //   sample.Sample$Processor$sendMessage

        // Get last class name, before inner class $
        String parts[] = cc.getName().split("\\$");
        String classParts[] = parts[0].split("\\.");
        String serverClassName = classParts[classParts.length - 1];

        // "Method" name is last:
        String methodName = parts[parts.length-1];

        return serverClassName + ":server:" + methodName;
    }
    
    public static final String CLASS_NAME = ThriftProcessFunctionInst.class.getName();
}
