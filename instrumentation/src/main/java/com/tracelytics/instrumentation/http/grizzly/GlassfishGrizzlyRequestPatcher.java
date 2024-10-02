package com.tracelytics.instrumentation.http.grizzly;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;


public class GlassfishGrizzlyRequestPatcher extends ClassInstrumentation {

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        if (cc.subtypeOf(classPool.get(GlassfishGrizzlyRequest.class.getName()))) { //parent class is already patched
            return false;
        }

        cc.addMethod(CtNewMethod.make("public String tvGetMethod() { return getMethod() != null ? getMethod().getMethodString() : null; }", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetRequestHeader(String key, String value) { getRequest().setHeader(key, value); }", cc));

        return tagInterface(cc, GlassfishGrizzlyRequest.class.getName());
    }

}