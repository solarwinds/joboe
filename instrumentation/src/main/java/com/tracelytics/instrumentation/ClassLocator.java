package com.tracelytics.instrumentation;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.tracelytics.ext.javassist.ClassPool;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

/**
 * Runs on a target class file to discover/locate other classes that are eligible for instrumentation.
 * 
 * This is useful for anonymous classes instrumentation as it's unsafe to add the anonymous class name directly to the ClassMap as the name might change
 * from version to version. For scala, locator is crucial as most functions for instrumentation are declared as anonymous classes that extents scala.Function 
 * 
 * 
 * This locator usually runs on the containing/declaring class of those target classes for instrumentation
 * 
 *  
 * @author pluk
 *
 */
public abstract class ClassLocator extends ClassInstrumentation
{
    protected static final Logger logger = LoggerFactory.getLogger();

    public void apply(CtClass cc, ClassPool classPool, String className) throws Exception {
        this.classPool = classPool;
        locateClasses(cc, className);
    }

    @Override
    /**
     * Do not allow class modification here. It is just a locator
     */
    protected final boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        return false;
    }
   
    /**
     * Discovers and register classes that should be instrumented
     * @param cc            containing/declaring class to look up the target classes
     * @param className     name of the containing/delcaring class
     * @throws Exception
     */
    protected abstract void locateClasses(CtClass cc, String className)
        throws Exception;
    
    /**
     * Get all nested classes including the non-immediate ones
     *
     * @param clazz
     *            the root class of the nested classes
     * @return
     */
    protected static Set<CtClass> getAllNestedClasses(CtClass clazz) {
        Set<CtClass> nestedClasses = new HashSet<CtClass>();
        try {
            for (CtClass nestedClass : clazz.getNestedClasses()) {
                nestedClasses.add(nestedClass);
                nestedClasses.addAll(getAllNestedClasses(nestedClass));
            }

        } catch (NotFoundException e) {
            logger.debug("Failed to get nested classes on [" + clazz.getName() + "], message : " + e.getMessage());
        }

        return nestedClasses;
    }
}
