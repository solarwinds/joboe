package com.tracelytics.instrumentation.sbt;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Patches <code>sbt.classpath.ClasspathFilter</code> to not filter on classes during instrumentation. The ClasspathFilter was designed to verify classes loaded through
 * the parent class loader is either from root or classpath. This however, triggers instrumentation error when we attempt to create instrumentation classes that subclass basic
 * scala classes such as <code>scala.runtime.AbstractFunction1</code> with explicit class lookup during the "toClass()" process in the case of multiple versions of scala libraries loaded.
 * 
 * In order to solve this issue, we need to patch sbt's ClassPathFilter to bypass the root/classpath check during instrumentation.
 *    
 * @author pluk
 *
 */
public class SbtClasspathFilterPatcher extends ClassInstrumentation {
    private static final String CLASS_NAME = SbtClasspathFilterPatcher.class.getName();

    private enum OpType { LOAD_CLASS }
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("loadClass", new String[] { "java.lang.String" }, "java.lang.Class", OpType.LOAD_CLASS)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        if (!cc.equals(classPool.get("sbt.classpath.ClasspathFilter"))) { //only patch the exact class
            return false;
        }
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, 
                    "if (" + CLASS_NAME + ".isInstrumentationInProgress($1)) {" //bypass the classpath filter check during instrumentation
                  + "    return super.loadClass($$);"
                  + "}", false);
                    
        }
        
        return true;
    }
    
    /**
     * Returns whether the current class loading happens within our java agent instrumentation stage (ClassInstrumentation.apply).
     * 
     * It currently checks the stack trace and match the ClassInstrumentation name. Getting stack trace is relatively expensive but since
     * this ClassPathFilter is not used commonly and class loading should only happen once per class, so the overhead is acceptable 
     * 
     * @param className
     * @return
     */
    public static boolean isInstrumentationInProgress(String className) {
        for (StackTraceElement stackFrame : Thread.currentThread().getStackTrace()) {
            if (ClassInstrumentation.class.getName().equals(stackFrame.getClassName())) {
                logger.debug("Bypassing the path check on sbt's ClasspathFilter during instrumentation, loading class " + className);
                return true;
            }
        }
        return false;
    }
}
