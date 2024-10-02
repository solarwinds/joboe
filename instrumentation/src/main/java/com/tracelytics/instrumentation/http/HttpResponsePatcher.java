package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;

public class HttpResponsePatcher extends ClassInstrumentation {

     public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        tagInterface(cc, HttpResponse.class.getName());

        cc.addMethod(CtNewMethod.make(
                "public String tvGetHeader(String headerName) { " +
                        "    if (headers() != null) {" +
                        "        return (String) headers().firstValue(headerName).orElse(null);" +
                        "    } else { " +
                        "        return null;" +
                        "    }" +
                        "}", cc));

        return true;
    }

}
