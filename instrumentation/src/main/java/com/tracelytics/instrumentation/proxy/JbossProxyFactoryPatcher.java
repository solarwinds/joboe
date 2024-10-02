package com.tracelytics.instrumentation.proxy;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ClassMap;
import com.tracelytics.instrumentation.MethodMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * Patches Jboss's `org.jboss.invocation.proxy.AbstractClassFactory` such that proxy class will be skipped
 * from instrumentation.
 *
 * Instrumenting jboss proxy class triggers VerifyError from the JVM. For details please refer to <placeholder></placeholder>
 */
public class JbossProxyFactoryPatcher extends ClassInstrumentation {
    private static final String CLASS_NAME = JbossProxyFactoryPatcher.class.getName();
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("defineClass", new String[] {}, "java.lang.Class", OpType.DEFINE_CLASS));

    private enum OpType {
        DEFINE_CLASS;
    }

    @Override
    protected boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (CtMethod ctMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(ctMethod, CLASS_NAME + ".recordProxyClass(getClassName());", false);
        }

        return true;
    }

    public static void recordProxyClass(String proxyClassName) {
        ClassMap.addExcludedType(proxyClassName);
    }
}
