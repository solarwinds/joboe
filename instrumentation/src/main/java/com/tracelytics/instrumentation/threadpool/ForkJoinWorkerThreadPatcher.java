package com.tracelytics.instrumentation.threadpool;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;

/**
 * Patches `ForkJoinWorkerThread` to avoid context inheritance, as context are being managed by `ForkJoinTaskPatcher`
 * 
 * @author pluk
 *
 */
public class ForkJoinWorkerThreadPatcher extends ClassInstrumentation {
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
    	for (CtConstructor constructor : cc.getConstructors()) {
    	    //flag Context to skip cloning context on the spawned worker thread as it is better handled in `ForkJoinTaskPatcher`
    	    insertBefore(constructor, Context.class.getName() + ".setSkipInheritingContext(true);", false); 
            insertAfter(constructor, Context.class.getName() + ".setSkipInheritingContext(false);", true, false); //reset the flag to resume the default
        }
    	return true;
    }
}

    