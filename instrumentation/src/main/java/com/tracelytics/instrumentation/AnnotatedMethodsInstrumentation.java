package com.tracelytics.instrumentation;

import java.util.List;

import com.tracelytics.ext.javassist.ClassPool;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.ext.javassist.bytecode.AnnotationDefaultAttribute;
import com.tracelytics.ext.javassist.bytecode.annotation.Annotation;
import com.tracelytics.ext.javassist.bytecode.annotation.BooleanMemberValue;
import com.tracelytics.ext.javassist.bytecode.annotation.MemberValue;
import com.tracelytics.ext.javassist.bytecode.annotation.StringMemberValue;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

/**
 * Instrumentation on a list of annotated methods that matches certain criteria
 * 
 * @author pluk
 *
 */
public abstract class AnnotatedMethodsInstrumentation {
    protected static final Logger logger = LoggerFactory.getLogger();
    
    public AnnotatedMethodsInstrumentation() {
        
    }
    
    public boolean apply(ClassPool classPool, CtClass cc, List<AnnotatedMethod> annotatedMethods) throws Exception {
        synchronized (cc) {
            if (!cc.isFrozen()) { //if a class is frozen, that means someone else has called cc.toBytecode or cc.toClass(), do not attempt to modify the byte code anymore
                return applyInstrumentation(classPool, cc, annotatedMethods);
            } else {
                logger.info("CtClass of [" + cc.getName() + "] is frozen. It probably has been modified already, skipping bytecode modification on this instance");
                return true; //should still return the byte code, as this class is modified elsewhere, we should not return null as it might override the modification in rare race condition
            }
        }
    }
    
    protected static AnnotationWithDefault getAnnotationWithDefault(Annotation annotation, ClassPool classPool) {
        try {
            return new AnnotationWithDefault(annotation, classPool.get(annotation.getTypeName()));
        } catch (NotFoundException e) {
            logger.warn("Failed to load annotation type for " + annotation.getTypeName() + " defaults cannot be retrieved");
            return new AnnotationWithDefault(annotation, null);
        }
    }
    
    /**
     * Contains the Javassist annotation and provides the handle to extract default value
     * @param annotation
     * @param classPool
     * @return
     */
    protected static class AnnotationWithDefault {
        private Annotation annotation;
        private CtClass annotationType;
        
        private AnnotationWithDefault(Annotation annotation, CtClass annotationType) {
            this.annotation = annotation;
            this.annotationType = annotationType;
        }
        
        /**
         * Gets the member value if defined; Otherwise, return the default value of the Annotation
         * @param memberName
         * @return
         */
        protected Object getMemberValue(String memberName) {
            MemberValue memberValue = annotation.getMemberValue(memberName);
            
            if (memberValue != null) {
                return getSupportedMemberValue(memberValue);
            } else { //try defaults
                if (annotationType != null) {
                    try {
                        CtMethod memberMethod = annotationType.getDeclaredMethod(memberName);
                        AnnotationDefaultAttribute defaultAttribute = (AnnotationDefaultAttribute) memberMethod.getMethodInfo().getAttribute(AnnotationDefaultAttribute.tag);
                        if (defaultAttribute != null) {
                            MemberValue defaultValue = defaultAttribute.getDefaultValue();
                            if (defaultValue != null) {
                                return getSupportedMemberValue(defaultValue);
                            }
                        }
                    } catch (NotFoundException e) {
                        logger.warn("Failed to load annotation value for " + annotation.getTypeName() + ", memberName " + memberName + " not found");
                    }
                }
            }
            
            return null;
        }
        
        private Object getSupportedMemberValue(MemberValue memberValue) {
            if (memberValue instanceof StringMemberValue) {
                return ((StringMemberValue)memberValue).getValue();
            } else if (memberValue instanceof BooleanMemberValue) {
                return ((BooleanMemberValue)memberValue).getValue();
            } else {
                logger.warn("Trying to retrieve annotation value of type " + memberValue.getClass().getName() + " is not supported");
                return null;
            }
        }
    }
    
    protected abstract boolean applyInstrumentation(ClassPool classPool, CtClass cc, List<AnnotatedMethod> annotatedMethods) throws Exception;

}
