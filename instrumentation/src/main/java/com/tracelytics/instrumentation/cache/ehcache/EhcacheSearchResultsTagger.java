package com.tracelytics.instrumentation.cache.ehcache;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Tags net.sf.ehcache.search.Results to expose its <code>size</code> method
 * @author pluk
 *
 */
public class EhcacheSearchResultsTagger extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        return tagInterface(cc, EhcacheSearchResults.class.getName());
    }
}