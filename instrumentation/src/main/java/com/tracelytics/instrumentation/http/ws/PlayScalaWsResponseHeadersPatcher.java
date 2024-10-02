package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.instrumentation.http.play.PlayBaseInstrumentation;


public class PlayScalaWsResponseHeadersPatcher extends PlayBaseInstrumentation {
 
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        //add method to get response header x-trace value
        cc.addMethod(CtNewMethod.make("public String tvGetXTraceHeaderValue() { "
                                    + "    scala.Option headerOption = headers().get(\"" + ServletInstrumentation.XTRACE_HEADER + "\");"
                                    + "    if (headerOption.isDefined()) {"
                                    + "        return (String)((scala.collection.Seq)headerOption.get()).apply(0);"
                                    + "    } else {"
                                    + "        return null;"
                                    + "    }"
                                    + "}", cc));
        
        try {
            cc.getDeclaredMethod("statusText");
        } catch (NotFoundException e) {
            cc.addMethod(CtNewMethod.make("public String statusText() { return null; }", cc));
        }
        
        tagInterface(cc, PlayScalaWsResponse.class.getName());
        return true;
    }
}