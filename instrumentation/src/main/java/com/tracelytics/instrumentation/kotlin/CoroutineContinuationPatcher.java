package com.tracelytics.instrumentation.kotlin;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtBehavior;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ContextPropagationPatcher;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Patches Koltin Coroutine Continuation, which represents code execution status that enables code execution pause/resumption around suspension points  
 * 
 * When a coroutine resumes (or even starts) operation, it calls the Continuation.resumeWith therefore we could propagate context when resumeWith is called  
 * 
 * @author pluk
 *
 */
public class CoroutineContinuationPatcher extends ClassInstrumentation {
    //private static final String CLASS_NAME = CoroutineContinuationPatcher.class.getName();
    private static final String PROPAGATION_CLASS_NAME = ContextPropagationPatcher.class.getName();

    private enum OpType {
        RESUME_WITH
    }

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("resumeWith", new String[]{"java.lang.Object"}, "void", OpType.RESUME_WITH)
    );


    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        boolean hasResumeWith = false;
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, PROPAGATION_CLASS_NAME + ".restoreContext(this, true, true);", false);
            insertAfter(method, PROPAGATION_CLASS_NAME + ".resetContext(this);", true, false);

            hasResumeWith = true;
        }

        if (hasResumeWith) {
            //only modify class if there's a concrete resumeWith
            for (CtBehavior behavior : cc.getDeclaredConstructors()) {
                insertAfter(behavior, PROPAGATION_CLASS_NAME + ".captureContext(this);", true, false);
            }
            addTvContextObjectAware(cc);
            addSpanAware(cc);

            return true;
        } else {
            return false;
        }
    }
}

    
