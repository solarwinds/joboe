package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.instrumentation.http.play.PlayBaseInstrumentation;


public class PlayScalaWsStreamedResponsePatcher extends PlayBaseInstrumentation {
 
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        //add method to get response header x-trace value
        cc.addMethod(CtNewMethod.make("public String tvGetXTraceHeaderValue() { "
                                    + "    if (headers() != null && headers().headers() != null) {"
                                    + "        scala.Option headerOption = headers().headers().get(\"" + ServletInstrumentation.XTRACE_HEADER + "\");"
                                    + "        if (headerOption.isDefined()) {"
                                    + "            scala.collection.Seq values = (scala.collection.Seq) headerOption.get();"
                                    + "            if (!values.isEmpty()) { "
                                    + "                return (String) values.apply(0);"
                                    + "            }"
                                    + "        }"
                                    + "    }"
                                    + "    return null;"
                                    + "}", cc));
        
        try {
            cc.getDeclaredMethod("status");
            logger.warn("Find unexpected method status() in case class play.api.libs.ws.StreamedResponse");
        } catch (NotFoundException e) {
            //expected, add the status method
            cc.addMethod(CtNewMethod.make("public int status() { return headers() != null ? headers().status() : -1; }", cc));
        }
        
        try {
            cc.getDeclaredMethod("statusText");
            logger.warn("Find unexpected method status() in case class play.api.libs.ws.StreamedResponse");
        } catch (NotFoundException e) {
            //expected, add the statusText method
            cc.addMethod(CtNewMethod.make("public String statusText() { return null; }", cc)); //statusText not available for StreamedResponse
        }
        
        tagInterface(cc, PlayScalaWsResponse.class.getName());
        return true;
    }
}