package com.tracelytics.instrumentation.http.akka.server;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.instrumentation.ClassLocator;
import com.tracelytics.instrumentation.ClassMap;
import com.tracelytics.instrumentation.Module;

/**
 * Locates the InHandler and `GraphicStageLogic` of `ControllerStage` in the `HttpServerBluePrint`.
 *
 * Registers the generated stage/logic classes for instrumentation
 *
 * @author pluk
 *
 */
public class AkkaHttpControllerStageFunctionLocator extends ClassLocator {

    @Override
    protected void locateClasses(CtClass cc, String className) throws Exception {
        CtClass inhandlerClass = classPool.get("akka.stream.stage.InHandler");
        CtClass graphStageLogicClass = classPool.get("akka.stream.stage.GraphStageLogic");

        for (CtClass nestedClass : getAllNestedClasses(cc)) {
            if (nestedClass.subtypeOf(inhandlerClass)) {
                ClassMap.registerInstrumentation(nestedClass.getName(), AkkaHttpControllerFunctionInstrumentation.class, Module.AKKA_HTTP); //instruments the controller function for entry and exit
            } else if (nestedClass.subtypeOf(graphStageLogicClass)) {
                ClassMap.registerInstrumentation(nestedClass.getName(), AkkaHttpControllerStageLogicInstrumentation.class, Module.AKKA_HTTP);
            }
        }
    }

}