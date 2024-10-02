package com.tracelytics.instrumentation.http.netty;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;

/**
 * Patches the Netty (4 and later versions) http response to provide response status code
 * @author pluk
 *
 */
public class Netty4HttpResponsePatcher extends NettyBaseInstrumentation {

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtClass httpMessageType = classPool.get("io.netty.handler.codec.http.DefaultHttpMessage");
        if (!cc.subtypeOf(httpMessageType)) { //do not modify other messages, such as GRPC might throw exceptions on setting header
            return false;
        }

        if (cc.subtypeOf(classPool.get(NettyHttpResponse.class.getName()))) {
            return false;
        }

        cc.addMethod(CtNewMethod.make("public Integer tvGetStatusCode() { return getStatus() != null ? Integer.valueOf(getStatus().code()) : null; }", cc));

        CtMethod tvSetHeaderMethod = CtNewMethod.make("public void tvSetHeader(String s, Object o) { if (headers() != null) { headers().set(s, o); }}", cc);
        
        cc.addMethod(tvSetHeaderMethod);
        
        cc.addMethod(CtNewMethod.make("public String tvGetHeader(String name) { return headers() != null ? headers().get(name) : null; }", cc));

        return tagInterface(cc, NettyHttpResponse.class.getName());
    }
    
    
}