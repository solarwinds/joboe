package com.tracelytics.instrumentation.http.akka.server;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassLocator;
import com.tracelytics.instrumentation.ClassMap;
import com.tracelytics.instrumentation.Module;

/**
 * Locates the `InHandler` of `PrepareRequests` stage in the `HttpServerBluePrint`.
 *
 * Registers the generated stage/pipeline class for instrumentation
 *
 * @author pluk
 */
public class AkkaHttpPrepareRequestsInHandlerLocator extends ClassLocator {
    @Override
    protected void locateClasses(CtClass cc, String className) throws Exception {
        CtClass inhandlerClass = classPool.get("akka.stream.stage.InHandler");

        for (CtClass nestedClass : getAllNestedClasses(cc)) {
            if (nestedClass.subtypeOf(inhandlerClass)) {
                ClassMap.registerInstrumentation(nestedClass.getName(), AkkaHttpPrepareRequestsInstrumentation.class, Module.AKKA_HTTP); //instruments the controller function for entry and exit
            }
        }
    }
}