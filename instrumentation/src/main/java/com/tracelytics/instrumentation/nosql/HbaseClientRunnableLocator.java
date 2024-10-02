package com.tracelytics.instrumentation.nosql;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassMap;
import com.tracelytics.instrumentation.Module;

/**
 * Search through <code>org.apache.hadoop.hbase.client.AsyncProcess</code> for the anonymous <code>Runnable</code> class, 
 * register those class in ClassMap for further instrumentation. This applies to Hbase version 0.96
 * 
 * @author Patson Luk
 *
 */
public class HbaseClientRunnableLocator extends HbaseBaseInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtClass runnableClass = classPool.get("java.lang.Runnable");
                
        for (CtClass nestedClass : cc.getNestedClasses()) {
            if (nestedClass.subtypeOf(runnableClass)) {
                ClassMap.registerInstrumentation(nestedClass.getName(), HbaseClientRunnableInstrumentation.class, Module.HBASE);
            }
        }
        
        
        return false; 
    }
}
