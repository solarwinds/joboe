package com.tracelytics.instrumentation.http.spray;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;

/**
 * Patches <code>spray.can.client.HttpClientConnection</code> to capture the remote host and ssl encryption info, such
 * those values can be reported in <code>SprayClientConnectionReceiveInstrumentation</code>
 *  
 * @author pluk
 *
 */
public class SprayClientConnectionPatcher extends ClassInstrumentation {
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<OpType>> contructorMatchers = Arrays.asList(
            new ConstructorMatcher<OpType>(new String[] { "java.lang.Object", "spray.can.Http$Connect" }, OpType.CTOR)
            );
    
    
    private enum OpType {
        RECEIVE, CTOR
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        cc.addField(CtField.make("private boolean tvSslEncryption;", cc));
        cc.addField(CtField.make("private java.net.InetSocketAddress tvRemoteAddress;", cc));
        cc.addMethod(CtNewMethod.make("public boolean tvGetSslEncryption() { return tvSslEncryption; }", cc));
        cc.addMethod(CtNewMethod.make("public java.net.InetSocketAddress tvGetRemoteAddress() { return tvRemoteAddress; }", cc));
        
        for (CtConstructor constructor : findMatchingConstructors(cc, contructorMatchers).keySet()) {
            insertAfter(constructor, 
                    "if ($2 != null) {"
                  + "   tvSslEncryption = $2.sslEncryption();"
                  + "   tvRemoteAddress = $2.remoteAddress();"
                  + "}", true);
        }
        tagInterface(cc, SprayHttpClientConnection.class.getName());
        return true;
    }
}