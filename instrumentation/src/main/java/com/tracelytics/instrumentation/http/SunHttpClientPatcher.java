package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;

/**
 * Patches sun/oracles <code>sun.net.www.http.HttpClient</code>, that when context is valid, tries to set x-trace header if not already set
 * 
 * Take note that this is necessary because of https://github.com/tracelytics/joboe/issues/458, that for redirects, oracle jdk 8 forbids setting customer request property for POST-GET redirect after
 * it clears all the existing header properties
 * 
 * @author pluk
 *
 */
public class SunHttpClientPatcher extends ClassInstrumentation {
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        for (CtMethod writeRequestsMethod : cc.getDeclaredMethods("writeRequests")) {
            if (shouldModify(cc, writeRequestsMethod)) {
                insertBefore(writeRequestsMethod, "if ($1 != null) { $1.setIfNotSet(\"" + ServletInstrumentation.XTRACE_HEADER + "\", " + Context.class.getName() + ".getMetadata().toHexString()); }");
            }
        }
        
        return true;
    }
}
