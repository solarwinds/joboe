package com.tracelytics.instrumentation.http.spray;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Tags the <code>spray.http.HttpResponse</code> to provide:
 * <ol>
 *  <li>Convenient method to get status code and the header of the response</li>
 * <ol> 
 * @author pluk
 *
 */
public class SprayHttpResponsePatcher extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        cc.addMethod(CtNewMethod.make(
                  "public int tvGetStatusCode() {"
                + "    if (status() != null) {"
                + "        return status().intValue();"
                + "    } else {"
                + "        return 0;"
                + "    }"
                + "}", cc)); 
        
        cc.addMethod(CtNewMethod.make(
                "public String tvGetResponseHeader(String key) {"
              + "    if (headers() != null) {"
              + "        for (int i = 0; i < headers().length(); i++) {"
              + "            spray.http.HttpHeader header = (spray.http.HttpHeader)headers().apply(i);"
              + "            if (header != null && key.equals(header.name())) {"
              + "                return header.value();"
              + "            }"
              + "        }"
              + "    }"
              + "    return null;"
              + "}",cc));
                
        
        tagInterface(cc, SprayHttpResponse.class.getName());        
        return true;
    }
}