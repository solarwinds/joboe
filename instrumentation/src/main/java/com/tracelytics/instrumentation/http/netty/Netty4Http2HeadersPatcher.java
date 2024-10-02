package com.tracelytics.instrumentation.http.netty;

import java.util.Iterator;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;

/**
 * Patches the netty http2 headers `io.netty.handler.codec.http2.Http2Headers` to provide handles to set/get headers.
 * 
 * Also added method to provide extra information such as uri and http method
 * 
 * @author pluk
 *
 */
public class Netty4Http2HeadersPatcher extends NettyBaseInstrumentation {
    private static final String CLASS_NAME = Netty4Http2HeadersPatcher.class.getName();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtClass headersType = classPool.get("io.netty.handler.codec.DefaultHeaders");
        if (!cc.subtypeOf(headersType)) { //do not modify other header types, such as GRPC might throw exceptions on setting header
            return false;
        }
        
        try {
            cc.getMethod("getUri", "()Ljava/lang/String;");
        } catch (NotFoundException e) { //expected
            cc.addMethod(CtNewMethod.make(
                    "public String getUri() { "
                  + "    try { "
                  + "        return path() != null ? path().toString() : null; "
                  + "    } catch (UnsupportedOperationException e) {"
                  + "        return " + CLASS_NAME + ".getWithName(this, \"uri\");"     
                  + "    }"
                  + "}", cc));
        }
        
        cc.addMethod(CtNewMethod.make("public Integer tvGetStatusCode() { "
                                    + "    String statusString;"
                                    + "    try { "
                                    + "        statusString = status() != null ? status().toString() : null;"
                                    + "    } catch (UnsupportedOperationException e) {"
                                    + "        statusString = " + CLASS_NAME + ".getWithName(this, \"status\");"     
                                    + "    }"
                                    + "    if (statusString != null) {"
                                    + "        try {"
                                    + "             return Integer.valueOf(statusString);"
                                    + "        } catch (NumberFormatException e) {"
                                    + "        }"
                                    + "    }"
                                    + "    return null;"
                                    + "}", cc));

        cc.addMethod(CtNewMethod.make(
                "public String getTvMethod() { "
                + "    try { "
                + "        return method() != null ? method().toString() : null; "
                + "    } catch (UnsupportedOperationException e) {"
                + "        return " + CLASS_NAME + ".getWithName(this, \"method\");"     
                + "    }"
                + "}", cc));
        
        CtMethod tvGetHeaderMethod = CtNewMethod.make(
                "public String tvGetHeader(String name) {"
              + "    name = name != null ? name.toLowerCase() : name;" //use lower case as netty converts the key to lower case for http/2
              + "    try { "
              + "        if (get(name) != null) { "
              + "            return get(name).toString(); "
              + "        } else { "
              + "            return null; "
              + "        }"
              + "    } catch (UnsupportedOperationException e) {"
              + "       return " + CLASS_NAME + ".getWithName(this, name);"     
              + "    }"
              + "}", cc);
        //set key to lower case, as http2 header validation throws error if the key contains upper case character
        CtMethod tvSetHeaderMethod = CtNewMethod.make("public void tvSetHeader(String s, Object o) { set(s != null ? s.toLowerCase() : s, o); }", cc);

        cc.addMethod(tvGetHeaderMethod);
        cc.addMethod(tvSetHeaderMethod);

        tagInterface(cc, NettyHttp2Headers.class.getName());
        return true;
    }
    
    public static String getWithName(Object headersObject, CharSequence name) {
        NettyHttp2Headers headers = (NettyHttp2Headers) headersObject;
        Iterator<Entry<CharSequence, CharSequence>> iterator = headers.iterator();
        while (iterator.hasNext()) {
            Entry<CharSequence, CharSequence> entry = iterator.next();
            if (entry.getKey().equals(name)) {
                return entry.getValue() != null ? entry.getValue().toString() : null;
            }
        }
        
        return null;
    }

}
