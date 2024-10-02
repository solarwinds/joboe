package com.tracelytics.instrumentation.http.netty;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;

/**
 * Patches the Netty (3 and earlier versions) http response to provide response status code
 * @author pluk
 *
 */
public class Netty3HttpResponsePatcher extends NettyBaseInstrumentation {

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        if (cc.subtypeOf(classPool.get(NettyHttpResponse.class.getName()))) {
            return false;
        }

        cc.addMethod(CtNewMethod.make("public Integer tvGetStatusCode() { return getStatus() != null ? Integer.valueOf(getStatus().getCode()) : null; }", cc));

        CtMethod tvSetHeaderMethod;
        CtMethod tvGetHeaderMethod;

        if (hasHeadersMethod(cc)) {
            tvSetHeaderMethod = CtNewMethod.make("public void tvSetHeader(String s, Object o) { if (headers() != null) { headers().set(s, o); }}", cc);
            tvGetHeaderMethod = CtNewMethod.make("public String tvGetHeader(String name) { return headers() != null ? headers().get(name) : null; }", cc);
        } else {
            tvSetHeaderMethod = CtNewMethod.make("public void tvSetHeader(String s, Object o) { setHeader(s, o); }", cc);
            tvGetHeaderMethod = CtNewMethod.make("public String tvGetHeader(String name) { return getHeader(name); }", cc);
        }

        cc.addMethod(tvSetHeaderMethod);
        cc.addMethod(tvGetHeaderMethod);
        
        return tagInterface(cc, NettyHttpResponse.class.getName());
    }
}