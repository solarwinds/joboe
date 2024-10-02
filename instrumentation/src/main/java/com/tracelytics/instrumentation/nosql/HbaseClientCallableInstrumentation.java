package com.tracelytics.instrumentation.nosql;

import java.util.Collections;
import java.util.Set;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Modifies Callable used in HBase such that it keeps track of the context during construction and then apply that context when call method is invoked
 * @author Patson Luk
 *
 */
public class HbaseClientCallableInstrumentation extends HbaseBaseInstrumentation {
    private static final String CLASS_NAME = HbaseClientCallableInstrumentation.class.getName();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        addTvContextObjectAware(cc);
        
        for (CtConstructor constructor : cc.getConstructors()) {
            insertAfter(constructor, CLASS_NAME + ".tagContext(this);", true, false); 
        }
        
        Set<CtMethod> methods = findMatchingMethods(cc, Collections.singletonList(new MethodMatcher<Object>("call", new String[] {}, "java.lang.Object"))).keySet();
        if (methods.size() != 1) {
            logger.warn("Expect exactly 1 call method, but found " + methods.size());
            return false;
        }
        
        CtMethod callMethod = methods.iterator().next();
        
        if (shouldModify(cc, callMethod)) {
            insertBefore(callMethod, CLASS_NAME + ".setContext(this);", false);
            insertAfter(callMethod, CLASS_NAME + ".clearContext(this);", true, false);
        }
        

        return true; 
    }
}
