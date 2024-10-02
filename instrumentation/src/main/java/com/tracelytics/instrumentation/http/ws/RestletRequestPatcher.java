package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Patches the <code>org.restlet.Request</code> to carry the context (x-trace id) as the handling of Http request header is in a different thread in Restlet (version 2.1.x)
 * @author pluk
 *
 */
public class RestletRequestPatcher extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        addTvContextObjectAware(cc);

        return true;
    }
}