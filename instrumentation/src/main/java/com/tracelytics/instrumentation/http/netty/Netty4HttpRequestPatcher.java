package com.tracelytics.instrumentation.http.netty;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;

/**
 * Patches the Netty (4 and later versions) http request to get the Http method.
 *
 * Also exposes various methods provided by netty http request
 *
 * @author pluk
 *
 */
public class Netty4HttpRequestPatcher extends NettyBaseInstrumentation {

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        CtClass httpMessageType = classPool.get("io.netty.handler.codec.http.DefaultHttpMessage");
        if (!cc.subtypeOf(httpMessageType)) { //do not modify other messages, such as GRPC might throw exceptions on setting header
            return false;
        }
        
        if (cc.subtypeOf(classPool.get(NettyHttpRequest.class.getName()))) {
            return false;
        }

        cc.addMethod(CtNewMethod.make("public String getTvMethod() { if (getMethod() != null) { return getMethod().name(); } else { return null; }}", cc));

        CtMethod tvGetHeaderMethod = CtNewMethod.make("public String tvGetHeader(String name) { if (headers() != null) { return headers().get(name); } else { return null; }}", cc);
        CtMethod tvSetHeaderMethod = CtNewMethod.make("public void tvSetHeader(String s, Object o) { if (headers() != null) { headers().set(s, o); }}", cc);

        cc.addMethod(tvGetHeaderMethod);
        cc.addMethod(tvSetHeaderMethod);

        return tagInterface(cc, NettyHttpRequest.class.getName());
    }

}