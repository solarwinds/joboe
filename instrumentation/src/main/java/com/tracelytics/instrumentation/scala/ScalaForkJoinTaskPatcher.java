package com.tracelytics.instrumentation.scala;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.tracelytics.ext.javassist.CtBehavior;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ContextPropagationPatcher;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Patches Scala's <code>ForkJoinTask</code> object such that it stores the context when the object is created or reinitialized, and restore context as a fork upon the "exec" method
 * @author pluk
 *
 */
public class ScalaForkJoinTaskPatcher extends ContextPropagationPatcher {
    private enum OpType {
    	EXEC, REINITIALIZE
    }
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> restoreContextMethodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("exec", new String[0], "boolean", OpType.EXEC)
    );
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> captureContextMethodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("reinitialize", new String[0], "void", OpType.REINITIALIZE)
    );
    
    
    
    
    @Override
    protected Collection<CtBehavior> getCaptureContextBehaviors(CtClass cc) {
        Collection<CtBehavior> behaviors = new ArrayList<CtBehavior>();
        behaviors.addAll(Arrays.asList(cc.getConstructors()));
        behaviors.addAll(findMatchingMethods(cc, captureContextMethodMatchers).keySet());
        return behaviors;
    }

    @Override
    protected Collection<CtMethod> getRestoreContextMethods(CtClass cc) {
        return findMatchingMethods(cc, restoreContextMethodMatchers).keySet();
    }
}

    
