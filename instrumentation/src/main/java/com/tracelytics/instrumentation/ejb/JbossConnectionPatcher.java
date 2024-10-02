package com.tracelytics.instrumentation.ejb;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Patches the <code>org.jboss.remoting3.Connection</code> to allow setting/getting of host and port, tags the {@link JbossConnection} to the class
 * @author pluk
 *
 */
public class JbossConnectionPatcher extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        cc.addField(CtField.make("private String tvHost;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetHost(String host) { tvHost = host; }", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetHost() { return tvHost; }", cc));
        
        cc.addField(CtField.make("private int tvPort;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetPort(int port) { tvPort = port; }", cc));
        cc.addMethod(CtNewMethod.make("public int tvGetPort() { return tvPort; }", cc));
        
        tagInterface(cc, JbossConnection.class.getName());

        return true;
    }
}