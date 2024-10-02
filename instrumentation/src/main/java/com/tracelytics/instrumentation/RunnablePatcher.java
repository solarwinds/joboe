package com.tracelytics.instrumentation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtMethod;

/**
 * Patches runnable object such that it stores the context when the object is created, and when the object is executed by the "run" method, context is restored as a fork
 * 
 * This class is only used by the Scala runnables for now
 * 
 * @author pluk
 *
 */
public class RunnablePatcher extends ContextPropagationPatcher {
    private enum OpType {
    	RUN
    }
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("run", new String[0], "void", OpType.RUN)
    );
    
    @Override
    protected Collection<CtConstructor> getCaptureContextBehaviors(CtClass cc) {
        return Arrays.asList(cc.getConstructors());
    }

    @Override
    protected Collection<CtMethod> getRestoreContextMethods(CtClass cc) {
        return findMatchingMethods(cc, methodMatchers).keySet();
    }
    
}

    
