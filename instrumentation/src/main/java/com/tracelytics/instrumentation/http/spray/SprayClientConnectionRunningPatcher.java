package com.tracelytics.instrumentation.http.spray;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Patches the "running" function declared in the <code>HttpUrlConnection</code> in order to tag whether SSL encryption is used on the <code>HttpRequest</code> object.
 * 
 * Such that when the <code>HttpRequest</code> object is instrumented, we can prepend the http:// or https:// prefix accordingly
 * 
 * @author pluk
 *
 */
public class SprayClientConnectionRunningPatcher extends ClassInstrumentation {

    private static String CLASS_NAME = SprayClientConnectionRunningPatcher.class.getName();
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("applyOrElse", new String[] { "java.lang.Object", "scala.Function1" }, "java.lang.Object", OpType.APPLY_OR_ELSE));
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<OpType>> contructorMatchers = Arrays.asList(
            new ConstructorMatcher<OpType>(new String[] { "spray.can.client.HttpClientConnection" }, OpType.CTOR));
    
    
    private enum OpType {
        APPLY_OR_ELSE, CTOR
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        cc.addField(CtField.make("private boolean tvSslEncryption;", cc));
        for (CtConstructor constructor : findMatchingConstructors(cc, contructorMatchers).keySet()) {
            insertAfter(constructor, 
                    "if ($1 instanceof " + SprayHttpClientConnection.class.getName() + ") {"
                  + "    tvSslEncryption = ((" + SprayHttpClientConnection.class.getName() + ")$1).tvGetSslEncryption();"
                  + "}", true, false);
        }

        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, CLASS_NAME + ".tagSslEncrypted($1, tvSslEncryption);", false);
        }
        return true;
    }
    
    public static void tagSslEncrypted(Object parameterObject, boolean sslEncryption) {
        if (parameterObject instanceof SprayHttpRequest) {
            ((SprayHttpRequest)parameterObject).tvSetSslEncryption(sslEncryption);
        }
    }
    
  
}