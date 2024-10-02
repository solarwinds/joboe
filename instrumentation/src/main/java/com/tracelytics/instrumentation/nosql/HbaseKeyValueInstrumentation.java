package com.tracelytics.instrumentation.nosql;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Tags Hbase class that provides Qualifier information
 * @author Patson Luk
 *
 */
public class HbaseKeyValueInstrumentation extends HbaseBaseInstrumentation {
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        
        tagInterface(cc, HbaseObjectWithQualifier.class.getName());

        return true;
    }
}