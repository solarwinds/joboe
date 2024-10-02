package com.tracelytics.instrumentation.http.play;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.*;
import com.tracelytics.instrumentation.Module;

/**
 * Locate wrapper class uses to wrap block (action logic Request => Result). Patch those wrapper class so we can find out the original block used in order to generate
 * Play profile controller/action KVs
 * @author pluk
 *
 */
public class PlayBlockWrapperLocator extends ClassLocator {
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<Object>> constructorMatchers = Arrays.asList(
      new ConstructorMatcher<Object>(new String[] { "java.lang.Object", "scala.Function1" }),
      new ConstructorMatcher<Object>(new String[] { "java.lang.Object", "scala.Function0" })
    );

    @Override
    protected void locateClasses(CtClass cc, String className) throws Exception {
        CtClass function0Class = classPool.get("scala.Function0");
        CtClass function1Class = classPool.get("scala.Function1");
        
        for (CtClass nestedClass : getAllNestedClasses(cc)) {
            if (nestedClass.subtypeOf(function0Class) || nestedClass.subtypeOf(function1Class)) {
              //ensure the ctor is there, we don't want to put irrelevant classes into the dynamic map, as each class can only has 1 instrumentation now,
              //putting class that is not a wrapper might prevent that class from being instrumented for other purposes
                if (!findMatchingConstructors(nestedClass, constructorMatchers).isEmpty()) {
                    ClassMap.registerInstrumentation(nestedClass.getName(), PlayBlockWrapperPatcher.class, Module.PLAY);
                }
            }
        }
    }
}
