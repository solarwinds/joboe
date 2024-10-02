package com.tracelytics.instrumentation.http.spray;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Instruments spray.routing.ExceptionHandler for exception thrown in the Spray routing module  
 * @author pluk
 *
 */
public class SprayExceptionInstrumentation extends ClassInstrumentation {

    private static String CLASS_NAME = SprayExceptionInstrumentation.class.getName();
    private static String LAYER_NAME = "spray-routing";
    
    private static ThreadLocal<Boolean> hasExceptionReportedThread = new ThreadLocal<Boolean>();

 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("apply", new String[] { "java.lang.Throwable" }, "scala.Function1", OpType.APPLY)
    );
    
    private enum OpType {
        APPLY
    }


    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, CLASS_NAME + ".reportException($1);");
            insertAfter(method, CLASS_NAME + ".clearThreadLocal();", true); //same exception might get nested calls, we need to avoid reporting the same exception multiple times
        }
        return true;
    }
    
    public static void reportException(Throwable exception) {
        if (hasExceptionReportedThread.get() == null) { //the exception might get applied multiple times, we only want to report it once
            ClassInstrumentation.reportError(LAYER_NAME, exception);
            hasExceptionReportedThread.set(true); 
        }
    }
    
    public static void clearThreadLocal() {
        hasExceptionReportedThread.remove();
    }
    
}