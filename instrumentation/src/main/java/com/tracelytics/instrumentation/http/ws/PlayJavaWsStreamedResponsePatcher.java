package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.http.play.PlayBaseInstrumentation;


public class PlayJavaWsStreamedResponsePatcher extends PlayBaseInstrumentation {
 
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        try {
            cc.getDeclaredMethod("getHeader");
        } catch (NotFoundException e) {
            //expected, add method to get response header
            cc.addMethod(CtNewMethod.make("public String getHeader(String key) { "
                                        + "    if (getHeaders() != null && getHeaders().getHeaders() != null) {"
                                        + "        java.util.List values =  (java.util.List) getHeaders().getHeaders().get(key);"
                                        + "        if (values != null && values.size() > 0) {"
                                        + "            return (String) values.get(0);"
                                        + "        }"
                                        + "    }"
                                        + "    return null;"
                                        + "}", cc));
        }
        
        try {
            cc.getDeclaredMethod("getStatus");
        } catch (NotFoundException e) {
            //expected, add method to get status
            cc.addMethod(CtNewMethod.make("public int getStatus() { "
                                        + "    if (getHeaders() != null) {"
                                        + "        return getHeaders().getStatus();"
                                        + "    }"
                                        + "    return -1;"
                                        + "}", cc));
        }
        
        try {
            cc.getDeclaredMethod("getStatusText");
        } catch (NotFoundException e) {
            //expected, add method to get null status text (dummy method, no status text info available for Streamed response)
            cc.addMethod(CtNewMethod.make("public String getStatusText() { "
                                        + "    return null;"
                                        + "}", cc));
        }
        
        tagInterface(cc, PlayJavaWsResponse.class.getName());
        return true;
    }
}