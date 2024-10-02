package com.tracelytics.instrumentation;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;

/**
 * Just a testing instrumentation to simply insert a field so we can verify after. Used by {@link ExcessiveClassLoaderInstrumentationTest}
 * @author pluk
 *
 */
public class DummyClassInstrumentation extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        cc.addField(CtField.make("public Object dummyField;", cc));
        return true;
    }
}