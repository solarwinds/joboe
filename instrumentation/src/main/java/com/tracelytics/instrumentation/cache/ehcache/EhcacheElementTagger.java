package com.tracelytics.instrumentation.cache.ehcache;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Tags "net.sf.ehcache.Element"
 * @author pluk
 *
 */
public class EhcacheElementTagger extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        return tagInterface(cc, EhcacheElement.class.getName());
    }
}