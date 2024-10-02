package com.tracelytics.instrumentation.http.netty;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;

/**
 * Patches the Netty (3 and earlier versions) http request to get the Http method
 *
 * Also exposes various methods provided by netty http request
 *
 * @author pluk
 *
 */
public class Netty3HttpRequestPatcher extends NettyBaseInstrumentation {

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        if (cc.subtypeOf(classPool.get(NettyHttpRequest.class.getName()))) {
            return false;
        }

        cc.addMethod(CtNewMethod.make("public String getTvMethod() { if (getMethod() != null) { return getMethod().getName(); } else { return null; }}", cc));

        CtMethod tvGetHeaderMethod;
        CtMethod tvSetHeaderMethod;

        if (hasHeadersMethod(cc)) {
            tvGetHeaderMethod = CtNewMethod.make("public String tvGetHeader(String name) { if (headers() != null) { return headers().get(name); } else { return null; }}", cc);
            tvSetHeaderMethod = CtNewMethod.make("public void tvSetHeader(String s, Object o) { if (headers() != null) { headers().set(s, o); }}", cc);
        } else {
            tvGetHeaderMethod = CtNewMethod.make("public String tvGetHeader(String name) { return getHeader(name); }", cc);
            tvSetHeaderMethod = CtNewMethod.make("public void tvSetHeader(String s,  Object o) { setHeader(s, o); }", cc);
        }

        cc.addMethod(tvGetHeaderMethod);
        cc.addMethod(tvSetHeaderMethod);

        return tagInterface(cc, NettyHttpRequest.class.getName());
    }

}