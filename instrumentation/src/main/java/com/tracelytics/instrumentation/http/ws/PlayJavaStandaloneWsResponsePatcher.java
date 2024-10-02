package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.http.play.PlayBaseInstrumentation;

public class PlayJavaStandaloneWsResponsePatcher extends PlayBaseInstrumentation {
 
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        try {
            cc.getDeclaredMethod("getHeader");
        } catch (NotFoundException e) {
            cc.addMethod(CtNewMethod.make("public String getHeader(String key) { "
                    + "        return (String) getSingleHeader(key).orElse(null);"
                    + "    }"
                    + "}", cc));
        }
        tagInterface(cc, PlayJavaWsResponse.class.getName());
        return true;
    }
}