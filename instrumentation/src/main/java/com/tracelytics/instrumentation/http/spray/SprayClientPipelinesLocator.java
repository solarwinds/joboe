package com.tracelytics.instrumentation.http.spray;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ClassLocator;
import com.tracelytics.instrumentation.ClassMap;
import com.tracelytics.instrumentation.Module;

/**
 * Locate the <code>Piplines</code> (commandPipeline (downstream) and eventPipline(upstream)) declared within the <code>spray.can.client.ClientFrontend$</code>
 * for instrumentation of the Http request/response traffic
 * 
 * @author pluk
 *
 */
public class SprayClientPipelinesLocator extends ClassLocator {
    @Override
    protected void locateClasses(CtClass cc, String className) throws Exception {
        CtClass pipelinesClass = classPool.get("spray.io.Pipelines");
        
        for (CtClass nestedClass : getAllNestedClasses(cc)) {
            if (nestedClass.subtypeOf(pipelinesClass)) {
                ClassMap.registerInstrumentation(nestedClass.getName(), SprayClientPipelinesInstrumentation.class, Module.SPRAY_CLIENT);
            }
        }
    }
}