package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;

/**
 * Tags the <code>org.apache.axis2.context.MessageContext</code> with our agent context
 * @author pluk
 *
 */
public class AxisWsMessageContextPatcher extends BaseWsClientInstrumentation {

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        addTvContextObjectAware(cc);
        return true;
    }
}
