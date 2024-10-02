package com.tracelytics.instrumentation.http.spray;

import java.util.HashMap;
import java.util.Map;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ClassLocator;
import com.tracelytics.instrumentation.ClassMap;
import com.tracelytics.instrumentation.Module;

/**
 * Locates the "receive" function declared within the <code>HttpClientConnection</code>
 * @author pluk
 *
 */
public class SprayClientConnectionReceiveLocator extends ClassLocator {
    @Override
    protected void locateClasses(CtClass cc, String className) throws Exception {
        CtClass partialFunctionClass = classPool.get("scala.runtime.AbstractPartialFunction");
        
        for (CtClass nestedClass : cc.getNestedClasses()) {
            if (nestedClass.subtypeOf(partialFunctionClass)) {
                if (nestedClass.getName().contains("receive")) {
                    ClassMap.registerInstrumentation(nestedClass.getName(), SprayClientConnectionReceiveInstrumentation.class, Module.SPRAY_CLIENT);
                } else if (nestedClass.getName().contains("running")) {
                    ClassMap.registerInstrumentation(nestedClass.getName(), SprayClientConnectionRunningPatcher.class, Module.SPRAY_CLIENT);
                }
            }
        }
    }
}