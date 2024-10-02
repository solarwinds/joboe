package com.tracelytics.instrumentation.cache.ehcache;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Tags org.hibernate.cache.CacheKey so we can recognize it w/o referencing the hibernate class directly
 * @author pluk
 *
 */
public class HibernateCacheKeyTagger extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        return tagInterface(cc, HibernateCacheKey.class.getName());
    }
}