package com.tracelytics.instrumentation.http.apache;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Instruments Apache Commons Http Client 4.x
 */
public class ApacheHttpMessageInstrumentation extends ClassInstrumentation {

     public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        tagInterface(cc, ApacheHttpMessage.class.getName());
        return true;
    }

}
