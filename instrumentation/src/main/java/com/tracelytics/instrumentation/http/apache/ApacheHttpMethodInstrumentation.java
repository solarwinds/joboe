package com.tracelytics.instrumentation.http.apache;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Instruments Apache Commons Http Client 3.x
 */
public class ApacheHttpMethodInstrumentation extends ClassInstrumentation {

     public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        tagInterface(cc, ApacheHttpMethod.class.getName());
        return true;
    }

}
