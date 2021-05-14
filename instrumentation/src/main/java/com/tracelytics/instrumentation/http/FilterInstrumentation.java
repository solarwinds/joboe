
/** Instruments javax.servlet.Filter : See http://docs.oracle.com/javaee/5/api/javax/servlet/Filter.html
 */

package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

import java.util.Arrays;
import java.util.List;

public class FilterInstrumentation extends ClassInstrumentation {
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("doFilter", new String[] { "javax.servlet.ServletRequest", "javax.servlet.ServletResponse", "javax.servlet.FilterChain"}, "void", OpType.DO_FILTER, true),
            new MethodMatcher<OpType>("doFilter", new String[] { "jakarta.servlet.ServletRequest", "jakarta.servlet.ServletResponse", "jakarta.servlet.FilterChain"}, "void", OpType.DO_FILTER, true)
    );

    private enum OpType { DO_FILTER }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            modifyFilter(method);
        }
        
        return true;
   
    }

    public void modifyFilter(CtMethod method)
            throws CannotCompileException, NotFoundException {
        // Filters are another HTTP entry point: we reuse servlet instrumentation.
        insertBefore(method, SERVLET_CLASS_NAME + ".layerEntry(this, (Object)$1,(Object)$2);", false);
        addErrorReporting(method, Throwable.class.getName(), layerName, classPool);
        insertAfter(method, SERVLET_CLASS_NAME + ".layerExit(this, (Object)$1,(Object)$2);", true, false);
    }

    static public final String SERVLET_CLASS_NAME = ServletInstrumentation.class.getName();


}
