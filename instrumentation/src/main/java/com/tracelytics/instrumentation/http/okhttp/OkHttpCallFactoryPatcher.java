package com.tracelytics.instrumentation.http.okhttp;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Patches `okhttp3.Call$Factory` to inject request entry x-trace ID. 
 * 
 * Take note that this happens before the call is actually executed (span entry), therefore we would need to pre-generate the x-trace ID (randomize op ID) so that span entry at {@link OkHttpCallbackInstrumentation} call use it for span entry 
 * @author Patson
 *
 */

public class OkHttpCallFactoryPatcher extends ClassInstrumentation {
    private static final String CLASS_NAME = OkHttpCallFactoryPatcher.class.getName();
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
        new MethodMatcher<MethodType>("newCall", new String[] { "okhttp3.Request" }, "java.lang.Object", MethodType.NEW_CALL)
    );
    
    private enum MethodType {
        NEW_CALL
    }
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
         
        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            insertBefore(method, "if (" + Context.class.getName() + ".isValid() && $1 != null && $1.header(\"" + ServletInstrumentation.XTRACE_HEADER + "\") == null) { "
                               + "    String generatedEntryXTrace = " + CLASS_NAME + ".generateEntryXTrace();"
                               + "    okhttp3.Request clonedRequest = $1.newBuilder().header(\"" + ServletInstrumentation.XTRACE_HEADER +"\", generatedEntryXTrace).build(); "
                               + "    $1 = clonedRequest;"
                               + "}", false);
        }        
        
        return true;
    }
    
    public static String generateEntryXTrace() {
        Metadata generatedMetadata = new Metadata(Context.getMetadata());
        generatedMetadata.randomizeOpID();
        return generatedMetadata.toHexString();
    }
}