package com.tracelytics.instrumentation.scala;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.instrumentation.ClassInstrumentation;

public class ScalaUtil {
    public static String EXECUTION_CONTEXT_FIELD = "tvExecutionContext";
    private ScalaUtil() {

    }
    /**
     * Add separate execute context for our instrumentation. So that our success/failure mapper function can run in its own context w/o impact from the default execution context
     * @param cc
     * @throws CannotCompileException
     */
    public static void addScalaContextForInstrumentation(CtClass cc) throws CannotCompileException {
        cc.addField(CtField.make("private static scala.concurrent.ExecutionContext " + EXECUTION_CONTEXT_FIELD + ";", cc));
        CtConstructor classInitializer = cc.getClassInitializer();
        if (classInitializer == null) {
            classInitializer = cc.makeClassInitializer();
        }
        ClassInstrumentation.insertAfter(classInitializer, EXECUTION_CONTEXT_FIELD + " = scala.concurrent.ExecutionContext$.MODULE$.fromExecutorService(java.util.concurrent.Executors.newCachedThreadPool());", true, false);
    }




}
