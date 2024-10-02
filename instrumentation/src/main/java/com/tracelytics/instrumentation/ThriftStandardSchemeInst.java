package com.tracelytics.instrumentation;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.expr.ExprEditor;
import com.tracelytics.ext.javassist.expr.MethodCall;
import com.tracelytics.joboe.Context;

/**
 *  Instruments classes extending org.apache.thrift.scheme.StandardScheme
 *  (Thrift-generated code, readers/writers for data structures)
 */
public class ThriftStandardSchemeInst extends ClassInstrumentation {

    /**
     * Instruments read and write methods
     * @param cc
     * @param className
     * @param classBytes
     * @return
     * @throws Exception
     */
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        try {
            // Add Thrift field for X-Trace ID:
            CtField xtField = CtField.make("public static final org.apache.thrift.protocol.TField TLYS_XTRACE_FIELD_DESC = " +
                                     "new org.apache.thrift.protocol.TField(\"xtrace\", " +
                                     "org.apache.thrift.protocol.TType.STRING, (short)999);", cc);
            cc.addField(xtField);

            // Modifiy read/write methods:
            CtMethod methods[] = cc.getDeclaredMethods();
            for(CtMethod method: methods) {
                if  (!method.getSignature().equals("(Lorg/apache/thrift/protocol/TProtocol;Lorg/apache/thrift/TBase;)V")) {
                    if (method.getName().equals("read")) {
                        modifyRead(method);
                    } else if (method.getName().equals("write")) {
                        modifyWrite(method);
                    }
                }

            }
        } catch(Exception ex) {
            logger.error("ERROR: Unable to instrument Thrift class " + className + ": " + ex.getMessage(), ex);
        }

        return true;
    }

    /**
     * Modifies Thrift "read" method to process X-Trace ID field
     * @param method
     * @throws CannotCompileException
     */
    private void modifyRead(CtMethod method) 
            throws CannotCompileException {
        
        method.instrument(new ExprEditor() {

            public void edit(MethodCall m) throws CannotCompileException {
                boolean found = false;

                // Looking for schemeField = iprot.readFieldBegin();
                if (m.getMethodName().equals("readFieldBegin") && !found) {

                    // Add code to look for X-Trace ID (field 999) at the start of the reader while loop.
                    // It would be cleaner to modify the switch statement, but we can't do that with javassist,
                    // at least not at a high level.
                    insertAfterMethodCall(m, 
                              "if ($_.id == 999 && $_.type == org.apache.thrift.protocol.TType.STRING) { " +
                              CLASS_NAME + ".setXTraceID($0.readString());" +
                              "    $0.readFieldEnd();" +
                              "    $_ = $0.readFieldBegin();" +
                              "}", false);
                    found = true;
                }

            }
        });
    }

    /** Called by instrumented read method (above): */
    public static void setXTraceID(String xTraceID) {
        try {
            Context.setMetadata(xTraceID); 
        } catch(Exception ex) { 
            logger.error("Thrift: Invalid X-TraceID: " + xTraceID, ex);
        }
    }

    /**
     * Modifies Thrift "write" to add X-Trace ID field
     * @param method
     */
    private void modifyWrite(CtMethod method)
        throws CannotCompileException {

        method.instrument(new ExprEditor() {

            public void edit(MethodCall m) throws CannotCompileException {
                boolean found = false;

                // Looking for oprot.writeFieldStop():
                if (m.getMethodName().equals("writeFieldStop") && !found) {
                    insertBeforeMethodCall(m, 
                              "if (com.tracelytics.joboe.Context.isValid()) { " +
                              "    $0.writeFieldBegin(TLYS_XTRACE_FIELD_DESC);" +
                              "    $0.writeString(com.tracelytics.joboe.Context.getMetadata().toHexString());" +
                              "    $0.writeFieldEnd();" +
                              "}", false);
                    found = true;
                }
            }
        });
    }

    public static final String CLASS_NAME = ThriftStandardSchemeInst.class.getName();
}