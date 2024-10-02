package com.tracelytics.instrumentation;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;

/**
 *  Instruments classes extending org.apache.thrift.transport.TSocket
 */
public class ThriftSocketInst extends ClassInstrumentation {
    public static final String CLASS_NAME = ThriftSocketInst.class.getName();
    
    private enum Type { HOST_PORT, SOCKET }
    
    @SuppressWarnings("unchecked")
    private static final List<ConstructorMatcher<Type>> constructorMatchers = Arrays.asList(new ConstructorMatcher<Type>(new String[] { "java.lang.String", "int"}, Type.HOST_PORT)
                                                                                          , new ConstructorMatcher<Type>(new String[] { "java.net.Socket"}, Type.SOCKET));
    /**
     * Instruments open method to report errors
     */
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        // Only modify the base class:
        if (!cc.getName().equals("org.apache.thrift.transport.TSocket")) {
            return false;    
        }

        try {
            CtMethod method = cc.getMethod("open", "()V");
            modifyOpenMethod(method, cc);
        } catch(Exception ex) {
            logger.error("ERROR: Unable to instrument Thrift class " + className + ": " + ex.getMessage());
        }
        
        tagSocket(cc);
        
        for (Entry<CtConstructor, Type> entry : findMatchingConstructors(cc, constructorMatchers).entrySet()) {
            CtConstructor constructor = entry.getKey();
            Type type = entry.getValue();
            if (type == Type.HOST_PORT) {
                insertAfter(constructor, "tvHost = $1; tvPort = $2;", true);
            } else if (type == Type.SOCKET) {
                insertAfter(constructor, "if ($1 != null && $1.getInetAddress() != null) { tvHost = $1.getInetAddress().getHostAddress();  tvPort = $1.getPort(); }", true);            
            }
        }
        
        return true;
    }

    private void tagSocket(CtClass cc)
        throws CannotCompileException, NotFoundException {
        cc.addField(CtField.make("private String tvHost;", cc));
        cc.addField(CtField.make("private int tvPort;", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetHost() { return tvHost; }", cc));
        cc.addMethod(CtNewMethod.make("public int tvGetPort() { return tvPort; }", cc));
        tagInterface(cc, ThriftTransportWithHost.class.getName());
    }

    private void modifyOpenMethod(CtMethod method, CtClass cc)
        throws CannotCompileException, NotFoundException {
        addErrorReporting(method, "java.lang.Throwable", null, classPool);
    }
}
