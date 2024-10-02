package com.tracelytics.instrumentation.http.akka.server;

import com.tracelytics.ext.javassist.CtBehavior;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassLocator;
import com.tracelytics.instrumentation.ClassMap;
import com.tracelytics.instrumentation.Module;

/**
 * Locates the default exception handler declared in the scala Object `akka.http.scaladsl.server.ExceptionHandler`
 *
 * Take note that such a default handler is used as a "catch all" when there's no custom defined handler to handle the exception
 */
public class AkkaHttpDefaultExceptionHandlerLocator extends ClassLocator {

    @Override
    protected void locateClasses(CtClass cc, String className) throws Exception {
        CtClass partialFunctionClass = classPool.get("scala.PartialFunction");

        for (CtClass nestedClass : getAllNestedClasses(cc)) {
            if (nestedClass.subtypeOf(partialFunctionClass)) {
                CtBehavior enclosingBehavior = nestedClass.getEnclosingBehavior();
                if (enclosingBehavior != null && "default".equals(enclosingBehavior.getName())) {
                    ClassMap.registerInstrumentation(nestedClass.getName(), AkkaHttpDefaultExceptionHandlerInstrumentation.class, Module.AKKA_HTTP);
                }
            }
        }
    }
}