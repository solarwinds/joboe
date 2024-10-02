package com.tracelytics.instrumentation.http.ws;


import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;

/**
 * Patches the <code>org.apache.axis2.description.OutInAxisOperationClient$NonBlockingInvocationWorker</code> to flag asynchronous operations. This worker submitted to the threadpool and later on
 * used for asynchronous Axis operations. 
 * @author pluk
 *
 */

public class AxisWsNonBlockingWorkerPatcher extends BaseWsClientInstrumentation {

    private static String CLASS_NAME = AxisWsNonBlockingWorkerPatcher.class.getName();
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<Object>> constructorMatcher = Arrays.asList(new ConstructorMatcher<Object>(new String[] { "java.lang.Object", "java.lang.Object", "org.apache.axis2.context.MessageContext" })); //1st param is the declaring class, implicit for ctor of inner class
    		
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        for (CtConstructor constructor : findMatchingConstructors(cc, constructorMatcher).keySet()) {
            insertAfter(constructor, CLASS_NAME + ".tagContext($3);", true); //1st param is the declaring class, implicit for ctor of inner class
        }
        
        return true;
    }
    
    /**
     * Tags the messageContext which is later used in another thread to send off the web service request
     * @param messageContext
     */
    public static void tagContext(Object messageContext) {
        if (messageContext instanceof TvContextObjectAware) {
            ((TvContextObjectAware) messageContext).setTvContext(Context.getMetadata());
        }
    }
}
