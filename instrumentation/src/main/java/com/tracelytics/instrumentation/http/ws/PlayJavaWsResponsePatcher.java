package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.http.play.PlayBaseInstrumentation;


public class PlayJavaWsResponsePatcher extends PlayBaseInstrumentation {
 
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        tagInterface(cc, PlayJavaWsResponse.class.getName());
        return true;
    }
}