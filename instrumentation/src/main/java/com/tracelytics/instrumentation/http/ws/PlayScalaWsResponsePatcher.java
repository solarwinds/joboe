package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.instrumentation.http.play.PlayBaseInstrumentation;


public class PlayScalaWsResponsePatcher extends PlayBaseInstrumentation {
 
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        //add method to get response header x-trace value
        cc.addMethod(CtNewMethod.make("public String tvGetXTraceHeaderValue() { "
                                    + "    scala.Option headerOption = header(\"" + ServletInstrumentation.XTRACE_HEADER + "\");"
                                    + "    if (headerOption.isDefined()) {"
                                    + "        return (String)headerOption.get();"
                                    + "    } else {"
                                    + "        return null;"
                                    + "    }"
                                    + "}", cc));
        
        tagInterface(cc, PlayScalaWsResponse.class.getName());
        return true;
    }
}