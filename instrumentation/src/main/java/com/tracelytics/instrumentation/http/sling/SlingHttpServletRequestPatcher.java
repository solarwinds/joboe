package com.tracelytics.instrumentation.http.sling;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Patches the {@code SlingHttpServletRequest} to provide Resource information
 * 
 * @author pluk
 *
 */
public class SlingHttpServletRequestPatcher extends ClassInstrumentation {
            
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        CtClass servletClass = classPool.get(SlingHttpServletRequest.class.getName());
        
        if (!cc.subtypeOf(servletClass)) {
            cc.addMethod(CtNewMethod.make("public " + SlingResource.class.getName() + " getTvResource() { " +
            		                      "    Object resource = getResource();" +
            		                      "    return (resource instanceof " + SlingResource.class.getName() + ") ? (" + SlingResource.class.getName() + ") resource : null;" +
            		                      "}", cc));
            
            tagInterface(cc, SlingHttpServletRequest.class.getName());
        }
        
        return true;
    }
}