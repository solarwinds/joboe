package com.tracelytics.instrumentation.http.akka.server;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Patches the `akka.http.javadsl.model.HttpResponse` to:
 * <ol>
 *  <li>Expose status code and header value</li>
 *  <li>Provide convenient method to add Header</li>
 * </ol>
 *
 * @author pluk
 *
 */
public class AkkaHttpResponsePatcher extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        boolean isJavaOptional;
        try {
            cc.getMethod("getHeader", "(Ljava/lang/String;)Ljava/util/Optional;");
            isJavaOptional = true;
        } catch (NotFoundException e) {
            isJavaOptional = false;
        }

        cc.addMethod(CtNewMethod.make("public int tvStatusCode() { return status() != null ? status().intValue() : -1 ; }", cc));
        cc.addMethod(CtNewMethod.make("public " + AkkaHttpResponse.class.getName() + " tvAddHeader(String key, String value) { return (" + AkkaHttpResponse.class.getName() + ") addHeader(new akka.http.scaladsl.model.headers.RawHeader(key, value)); }", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetHeader(String key) { return (String) (getHeader(key)." + (isJavaOptional ? "isPresent" : "isDefined") + "() ? ((akka.http.javadsl.model.HttpHeader) getHeader(key).get()).value() : null); }", cc));

        tagInterface(cc, AkkaHttpResponse.class.getName());

        return true;
    }
}