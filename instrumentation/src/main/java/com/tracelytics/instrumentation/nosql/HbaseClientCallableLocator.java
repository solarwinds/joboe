package com.tracelytics.instrumentation.nosql;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassMap;
import com.tracelytics.instrumentation.Module;

/**
 * Search through <code>org.apache.hadoop.hbase.client.HConnectionManager$HConnectionImplementation</code> for the anonymous <code>Callable</code> class, 
 * register those class in ClassMap for further instrumentation. This applies to Hbase version 0.94 or earlier
 * 
 * @author Patson Luk
 *
 */
public class HbaseClientCallableLocator extends HbaseBaseInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtClass callableClass = classPool.get("java.util.concurrent.Callable");
                
        for (CtClass nestedClass : cc.getNestedClasses()) {
            if (nestedClass.subtypeOf(callableClass)) {
                ClassMap.registerInstrumentation(nestedClass.getName(), HbaseClientCallableInstrumentation.class, Module.HBASE);
            }
        }
        
        
        return false; 
    }
}
