package com.tracelytics.instrumentation.http.sling;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Patches the Sling {@code Resource} in order to extract information such as resource path and resource type
 * 
 * @author pluk
 *
 */
public class SlingResourcePatcher extends ClassInstrumentation {
            
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        CtClass resourceClass = classPool.get(SlingResource.class.getName());
        
        if (!cc.subtypeOf(resourceClass)) {
            tagInterface(cc, SlingResource.class.getName());
        }
        
        return true;
    }
}